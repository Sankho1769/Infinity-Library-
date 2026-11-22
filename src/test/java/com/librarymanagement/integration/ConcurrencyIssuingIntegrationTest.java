package com.librarymanagement.integration;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.dto.CirculationDTOs.IssueBookRequest;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.circulation.service.CirculationService;
import com.librarymanagement.common.exception.ConflictException;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIssuingIntegrationTest {

    @Autowired private CirculationService circulationService;
    @Autowired private LoanRepository loanRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private LibraryRepository libraryRepository;

    private Long bookCopyId;
    private Long memberId1;
    private Long memberId2;

    @BeforeEach
    void setupTestData() {
        Library library = libraryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Library lib = new Library();
            lib.setName("Integration Library");
            lib.setCode("INT-LIB-01");
            return libraryRepository.save(lib);
        });

        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);

        Book book = new Book();
        book.setTitle("Concurrency Mastery " + suffix);
        book.setIsbn("978-" + suffix);
        book.setAuthor("Brian Goetz");
        book.setGenre("Computer Science");
        Book savedBook = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(savedBook);
        copy.setLibrary(library);
        copy.setBarcode("CONC-CPY-" + suffix);
        copy.setStatus(BookCopyStatus.AVAILABLE);
        BookCopy savedCopy = bookCopyRepository.save(copy);
        this.bookCopyId = savedCopy.getId();

        Member m1 = new Member();
        m1.setName("Alice Engineer");
        m1.setEmail("alice-" + suffix + "@conc.test");
        m1.setMembershipNumber("MEM-CONC-1-" + suffix);
        m1.setStatus(MemberStatus.ACTIVE);
        m1.setMembershipStartDate(LocalDate.now());
        m1.setMembershipExpiryDate(LocalDate.now().plusYears(1));
        Member savedM1 = memberRepository.save(m1);
        this.memberId1 = savedM1.getId();

        Member m2 = new Member();
        m2.setName("Bob Architect");
        m2.setEmail("bob-" + suffix + "@conc.test");
        m2.setMembershipNumber("MEM-CONC-2-" + suffix);
        m2.setStatus(MemberStatus.ACTIVE);
        m2.setMembershipStartDate(LocalDate.now());
        m2.setMembershipExpiryDate(LocalDate.now().plusYears(1));
        Member savedM2 = memberRepository.save(m2);
        this.memberId2 = savedM2.getId();
    }

    @Test
    @DisplayName("CRITICAL CONCURRENCY INVARIANT: Competing simultaneous issue requests for the final available copy result in exactly one success and one conflict")
    void testConcurrentBookIssuanceOnlyOneSucceeds() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Librarian A tries to issue copy to Member 1
        executorService.submit(() -> {
            try {
                startSignal.await();
                circulationService.issueBook(new IssueBookRequest(memberId1, bookCopyId, null), null, "librarian_a");
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                doneSignal.countDown();
            }
        });

        // Thread 2: Librarian B simultaneously tries to issue same copy to Member 2
        executorService.submit(() -> {
            try {
                startSignal.await();
                circulationService.issueBook(new IssueBookRequest(memberId2, bookCopyId, null), null, "librarian_b");
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                doneSignal.countDown();
            }
        });

        // Fire both threads simultaneously
        startSignal.countDown();
        boolean completed = doneSignal.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();

        // Exactly ONE transaction must succeed, and the competing transaction MUST be rejected with Conflict
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Verify Database Invariants
        BookCopy copyAfter = bookCopyRepository.findById(bookCopyId).orElseThrow();
        assertThat(copyAfter.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);

        long activeLoansForCopy = loanRepository.countByMemberIdAndStatus(memberId1, LoanStatus.ACTIVE)
                + loanRepository.countByMemberIdAndStatus(memberId2, LoanStatus.ACTIVE);
        assertThat(activeLoansForCopy).isEqualTo(1);
    }
}
