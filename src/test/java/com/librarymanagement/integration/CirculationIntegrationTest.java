package com.librarymanagement.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.dto.CirculationDTOs.IssueBookRequest;
import com.librarymanagement.circulation.dto.CirculationDTOs.ReturnBookRequest;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.common.security.JwtTokenProvider;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import com.librarymanagement.members.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CirculationIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private ObjectMapper objectMapper;

    private String librarianToken;
    private Long memberId;
    private Long bookCopyId;

    @BeforeEach
    void setUpTestData() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Library library = libraryRepository.findAll().stream().findFirst().orElseThrow();

        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);

        Member member = new Member();
        member.setName("Test Borrower");
        member.setEmail("borrower-" + suffix + "@university.edu");
        member.setMembershipNumber("MEM-CIRC-" + suffix);
        member.setStatus(MemberStatus.ACTIVE);
        member.setMembershipStartDate(LocalDate.now());
        member.setMembershipExpiryDate(LocalDate.now().plusYears(1));
        Member savedMember = memberRepository.save(member);
        this.memberId = savedMember.getId();

        Book book = new Book();
        book.setTitle("Integration Testing " + suffix);
        book.setIsbn("978-" + suffix);
        book.setAuthor("Robert C. Martin");
        book.setGenre("Software Engineering");
        Book savedBook = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(savedBook);
        copy.setLibrary(library);
        copy.setBarcode("CIRC-CPY-" + suffix);
        copy.setStatus(BookCopyStatus.AVAILABLE);
        BookCopy savedCopy = bookCopyRepository.save(copy);
        this.bookCopyId = savedCopy.getId();

        UserPrincipal staffPrincipal = new UserPrincipal(999L, "head_librarian", "pass", "LIBRARIAN", null, true);
        this.librarianToken = "Bearer " + jwtTokenProvider.generateAccessToken(staffPrincipal);
    }

    @Test
    @DisplayName("Complete circulation lifecycle: Issue book -> verify ON_LOAN -> Return book -> verify AVAILABLE -> Return again fails")
    void testCompleteCirculationLifecycle() throws Exception {
        // 1. Issue Book
        IssueBookRequest issueReq = new IssueBookRequest(memberId, bookCopyId, null);
        String issueResponseJson = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", librarianToken)
                        .header("Idempotency-Key", "idemp-key-lifecycle-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.barcode").isString())
                .andReturn().getResponse().getContentAsString();

        Long loanId = objectMapper.readTree(issueResponseJson).get("data").get("id").asLong();

        // Verify copy status is now ON_LOAN
        BookCopy copyAfterIssue = bookCopyRepository.findById(bookCopyId).orElseThrow();
        assertThat(copyAfterIssue.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);

        // 2. Return Book
        ReturnBookRequest returnReq = new ReturnBookRequest(loanId, null);
        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return")
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loan.status").value("RETURNED"))
                .andExpect(jsonPath("$.data.overdue").value(false));

        // Verify copy status is restored to AVAILABLE
        BookCopy copyAfterReturn = bookCopyRepository.findById(bookCopyId).orElseThrow();
        assertThat(copyAfterReturn.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);

        // 3. Duplicate Return attempt must fail with 409 CONFLICT (LOAN_ALREADY_RETURNED)
        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return")
                        .header("Authorization", librarianToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LMS-409-03"));

        // 4. Verify Reports accurately reflect inventory
        mockMvc.perform(get("/api/v1/reports/inventory")
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableCopies").isNumber());
    }
}
