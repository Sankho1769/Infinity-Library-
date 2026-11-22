package com.librarymanagement.service;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.dto.CatalogDTOs.*;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.catalog.service.BookService;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.ConflictException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private LibraryRepository libraryRepository;
    @Mock private AuditService auditService;

    private BookService bookService;
    private Library library;
    private Book book;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, bookCopyRepository, libraryRepository, auditService);

        library = new Library();
        library.setId(1L);
        library.setName("Central Library");

        book = new Book();
        book.setId(10L);
        book.setTitle("Domain-Driven Design");
        book.setIsbn("978-0321125217");
        book.setAuthor("Eric Evans");
        book.setGenre("Software Architecture");
        book.setActive(true);
    }

    @Test
    @DisplayName("Register new bibliographic book and initial physical copies")
    void testRegisterBookSuccess() {
        when(bookRepository.existsByIsbn("978-0321125217")).thenReturn(false);
        when(libraryRepository.findById(1L)).thenReturn(Optional.of(library));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        CreateBookRequest request = new CreateBookRequest(
                "Domain-Driven Design",
                "978-0321125217",
                "Eric Evans",
                "Addison-Wesley",
                2003,
                "Software Architecture",
                "English",
                "Tackling complexity in software heart.",
                3
        );

        BookResponse response = bookService.registerBook(request, "librarian");

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Domain-Driven Design");
        verify(bookCopyRepository, times(3)).save(any(BookCopy.class));
    }

    @Test
    @DisplayName("Register book with duplicate ISBN throws ConflictException")
    void testRegisterDuplicateIsbnThrowsConflict() {
        when(bookRepository.existsByIsbn("978-0321125217")).thenReturn(true);

        CreateBookRequest request = new CreateBookRequest(
                "Domain-Driven Design",
                "978-0321125217",
                "Eric Evans",
                null, 2003, "Software Architecture", "English", null, 1
        );

        assertThatThrownBy(() -> bookService.registerBook(request, "librarian"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Get book details with live copy inventory counts")
    void testGetBookDetails() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookCopyRepository.countByBookId(10L)).thenReturn(5L);
        when(bookCopyRepository.countByBookIdAndStatus(10L, BookCopyStatus.AVAILABLE)).thenReturn(3L);
        when(bookCopyRepository.countByBookIdAndStatus(10L, BookCopyStatus.ON_LOAN)).thenReturn(2L);
        when(bookCopyRepository.countByBookIdAndStatus(10L, BookCopyStatus.RESERVED)).thenReturn(0L);

        BookResponse response = bookService.getBookDetails(10L);

        assertThat(response.totalCopies()).isEqualTo(5L);
        assertThat(response.availableCopies()).isEqualTo(3L);
        assertThat(response.issuedCopies()).isEqualTo(2L);
    }
}
