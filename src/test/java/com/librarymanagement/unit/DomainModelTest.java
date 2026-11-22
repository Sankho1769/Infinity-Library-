package com.librarymanagement.unit;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    @DisplayName("Member eligibility should require ACTIVE status and non-expired membership date")
    void testMemberEligibility() {
        Member activeMember = new Member();
        activeMember.setStatus(MemberStatus.ACTIVE);
        activeMember.setMembershipExpiryDate(LocalDate.now().plusMonths(6));
        assertThat(activeMember.isBorrowingEligible()).isTrue();

        Member suspendedMember = new Member();
        suspendedMember.setStatus(MemberStatus.SUSPENDED);
        suspendedMember.setMembershipExpiryDate(LocalDate.now().plusMonths(6));
        assertThat(suspendedMember.isBorrowingEligible()).isFalse();

        Member expiredMember = new Member();
        expiredMember.setStatus(MemberStatus.ACTIVE);
        expiredMember.setMembershipExpiryDate(LocalDate.now().minusDays(1));
        assertThat(expiredMember.isBorrowingEligible()).isFalse();

        Member inactiveMember = new Member();
        inactiveMember.setStatus(MemberStatus.INACTIVE);
        assertThat(inactiveMember.isBorrowingEligible()).isFalse();
    }

    @Test
    @DisplayName("Book and BookCopy status invariants and bibliographic separation")
    void testBookAndCopyRelationship() {
        Book book = new Book();
        book.setTitle("Designing Data-Intensive Applications");
        book.setIsbn("978-1449373320");
        book.setAuthor("Martin Kleppmann");
        book.setGenre("Distributed Systems");

        Library library = new Library();
        library.setName("Central Library");

        BookCopy copy1 = new BookCopy();
        copy1.setBook(book);
        copy1.setLibrary(library);
        copy1.setBarcode("DDIA-001");
        copy1.setStatus(BookCopyStatus.AVAILABLE);

        BookCopy copy2 = new BookCopy();
        copy2.setBook(book);
        copy2.setLibrary(library);
        copy2.setBarcode("DDIA-002");
        copy2.setStatus(BookCopyStatus.AVAILABLE);

        assertThat(copy1.getBook().getTitle()).isEqualTo(copy2.getBook().getTitle());
        assertThat(copy1.getBarcode()).isNotEqualTo(copy2.getBarcode());
        assertThat(copy1.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);

        copy1.setStatus(BookCopyStatus.ON_LOAN);
        assertThat(copy1.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);
        assertThat(copy2.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Loan preserves historical snapshot of policies at issuance")
    void testLoanPolicySnapshot() {
        Member member = new Member();
        member.setName("Dr. Elena");

        BookCopy copy = new BookCopy();
        copy.setBarcode("DDIA-001");

        Instant issueDate = Instant.now();
        Instant dueDate = issueDate.plusSeconds(14 * 86400);

        Loan loan = new Loan(member, copy, issueDate, dueDate, 14, new BigDecimal("5.00"));
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getLoanDurationDays()).isEqualTo(14);
        assertThat(loan.getDailyFineRateSnapshot()).isEqualByComparingTo("5.00");
    }
}
