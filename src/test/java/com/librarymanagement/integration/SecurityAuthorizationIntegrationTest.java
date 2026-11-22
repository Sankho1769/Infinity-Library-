package com.librarymanagement.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.common.security.JwtTokenProvider;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.fines.dto.FineDTOs.WaiveFineRequest;
import com.librarymanagement.fines.repository.FineRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SecurityAuthorizationIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private FineRepository fineRepository;
    @Autowired private ObjectMapper objectMapper;

    private String member1Token;
    private String member2Token;
    private String librarianToken;
    private Long member1Id;
    private Long member2Id;
    private Long loanMember2Id;
    private Long fineMember2Id;

    @BeforeEach
    void setUpSecurityFixtures() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Library library = libraryRepository.findAll().stream().findFirst().orElseThrow();

        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);

        Member m1 = new Member();
        m1.setName("Member One");
        m1.setEmail("m1-" + suffix + "@sec.org");
        m1.setMembershipNumber("MEM-SEC-1-" + suffix);
        m1.setStatus(MemberStatus.ACTIVE);
        m1.setMembershipStartDate(LocalDate.now());
        m1.setMembershipExpiryDate(LocalDate.now().plusYears(1));
        Member savedM1 = memberRepository.save(m1);
        this.member1Id = savedM1.getId();

        Member m2 = new Member();
        m2.setName("Member Two");
        m2.setEmail("m2-" + suffix + "@sec.org");
        m2.setMembershipNumber("MEM-SEC-2-" + suffix);
        m2.setStatus(MemberStatus.ACTIVE);
        m2.setMembershipStartDate(LocalDate.now());
        m2.setMembershipExpiryDate(LocalDate.now().plusYears(1));
        Member savedM2 = memberRepository.save(m2);
        this.member2Id = savedM2.getId();

        Book book = new Book();
        book.setTitle("Security Architecture " + suffix);
        book.setIsbn("978-" + suffix);
        book.setAuthor("Martin Fowler");
        book.setGenre("Security");
        Book savedBook = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(savedBook);
        copy.setLibrary(library);
        copy.setBarcode("SEC-CPY-" + suffix);
        copy.setStatus(BookCopyStatus.ON_LOAN);
        BookCopy savedCopy = bookCopyRepository.save(copy);

        Loan loanM2 = new Loan(savedM2, savedCopy, Instant.now(), Instant.now().plusSeconds(86400), 14, new BigDecimal("5.00"));
        Loan savedLoanM2 = loanRepository.save(loanM2);
        this.loanMember2Id = savedLoanM2.getId();

        Fine fineM2 = new Fine(savedLoanM2, savedM2, new BigDecimal("25.00"), LocalDate.now());
        Fine savedFineM2 = fineRepository.save(fineM2);
        this.fineMember2Id = savedFineM2.getId();

        // Generate JWT tokens for authentication
        UserPrincipal p1 = new UserPrincipal(101L, "user_m1", "pass", "MEMBER", member1Id, true);
        this.member1Token = "Bearer " + jwtTokenProvider.generateAccessToken(p1);

        UserPrincipal p2 = new UserPrincipal(102L, "user_m2", "pass", "MEMBER", member2Id, true);
        this.member2Token = "Bearer " + jwtTokenProvider.generateAccessToken(p2);

        UserPrincipal librarianPrincipal = new UserPrincipal(103L, "librarian_user", "pass", "LIBRARIAN", null, true);
        this.librarianToken = "Bearer " + jwtTokenProvider.generateAccessToken(librarianPrincipal);
    }

    @Test
    @DisplayName("OBJECT-LEVEL AUTHORIZATION (BOLA/IDOR): Member 1 cannot view Member 2's loan details")
    void testMemberCannotViewAnotherMembersLoan() throws Exception {
        mockMvc.perform(get("/api/v1/loans/" + loanMember2Id)
                        .header("Authorization", member1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Access denied: cannot access another member's loan records"));
    }

    @Test
    @DisplayName("OBJECT-LEVEL AUTHORIZATION (BOLA/IDOR): Member 1 cannot view Member 2's loans list via /members/{id}/loans")
    void testMemberCannotQueryAnotherMembersLoansList() throws Exception {
        mockMvc.perform(get("/api/v1/members/" + member2Id + "/loans")
                        .header("Authorization", member1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OBJECT-LEVEL AUTHORIZATION (BOLA/IDOR): Member 1 cannot view Member 2's fine details")
    void testMemberCannotViewAnotherMembersFine() throws Exception {
        mockMvc.perform(get("/api/v1/fines/" + fineMember2Id)
                        .header("Authorization", member1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ROLE-BASED AUTHORIZATION: Regular member cannot issue books (Staff Only)")
    void testMemberCannotIssueBook() throws Exception {
        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", member1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": " + member1Id + ", \"bookId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE-BASED AUTHORIZATION: Regular member cannot waive fines (Staff Only)")
    void testMemberCannotWaiveFine() throws Exception {
        WaiveFineRequest request = new WaiveFineRequest(new BigDecimal("10.00"), "Unauthorized waiver attempt");
        mockMvc.perform(post("/api/v1/fines/" + fineMember2Id + "/waive")
                        .header("Authorization", member1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LIBRARIAN can view any member's loan details and fines")
    void testLibrarianAuthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/loans/" + loanMember2Id)
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(loanMember2Id));

        mockMvc.perform(get("/api/v1/fines/" + fineMember2Id)
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fineMember2Id));
    }
}
