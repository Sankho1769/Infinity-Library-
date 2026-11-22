package com.librarymanagement.catalog.service;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.dto.CatalogDTOs.*;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.ConflictException;
import com.librarymanagement.common.exception.ErrorCode;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.common.response.PagedResponse;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LibraryRepository libraryRepository;
    private final AuditService auditService;

    public BookService(BookRepository bookRepository,
                       BookCopyRepository bookCopyRepository,
                       LibraryRepository libraryRepository,
                       AuditService auditService) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.libraryRepository = libraryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BookResponse registerBook(CreateBookRequest request, String actor) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new ConflictException(ErrorCode.INVALID_OPERATION, "Book with ISBN " + request.isbn() + " already exists");
        }

        Library defaultLib = libraryRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Default Library not found"));

        Book book = new Book();
        book.setTitle(request.title());
        book.setIsbn(request.isbn());
        book.setAuthor(request.author());
        book.setPublisher(request.publisher());
        book.setPublicationYear(request.publicationYear());
        book.setGenre(request.genre());
        if (request.language() != null) book.setLanguage(request.language());
        book.setDescription(request.description());
        book.setActive(true);

        Book savedBook = bookRepository.save(book);

        // Add physical inventory copies
        int copiesToAdd = Math.max(1, request.initialCopies());
        for (int i = 1; i <= copiesToAdd; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(savedBook);
            copy.setLibrary(defaultLib);
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setAcquisitionDate(LocalDate.now());
            copy.setBarcode("CPY-" + savedBook.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            bookCopyRepository.save(copy);
        }

        auditService.record(actor, "REGISTER_BOOK", "Book", savedBook.getId().toString(), null, "Created book: " + savedBook.getTitle());

        return getBookDetails(savedBook.getId());
    }

    @Transactional(readOnly = true)
    public BookResponse getBookDetails(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));

        long total = bookCopyRepository.countByBookId(id);
        long available = bookCopyRepository.countByBookIdAndStatus(id, BookCopyStatus.AVAILABLE);
        long issued = bookCopyRepository.countByBookIdAndStatus(id, BookCopyStatus.ON_LOAN);
        long reserved = bookCopyRepository.countByBookIdAndStatus(id, BookCopyStatus.RESERVED);

        return BookResponse.of(book, total, available, issued, reserved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookResponse> searchBooks(String query, String genre, Pageable pageable) {
        Page<Book> page = bookRepository.searchBooks(query, genre, pageable);
        Page<BookResponse> dtoPage = page.map(book -> {
            long total = bookCopyRepository.countByBookId(book.getId());
            long available = bookCopyRepository.countByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE);
            long issued = bookCopyRepository.countByBookIdAndStatus(book.getId(), BookCopyStatus.ON_LOAN);
            long reserved = bookCopyRepository.countByBookIdAndStatus(book.getId(), BookCopyStatus.RESERVED);
            return BookResponse.of(book, total, available, issued, reserved);
        });
        return PagedResponse.from(dtoPage);
    }

    @Transactional
    public BookResponse updateBook(Long id, UpdateBookRequest request, String actor) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPublisher(request.publisher());
        book.setPublicationYear(request.publicationYear());
        book.setGenre(request.genre());
        if (request.language() != null) book.setLanguage(request.language());
        book.setDescription(request.description());
        book.setUpdatedAt(Instant.now());

        bookRepository.save(book);
        auditService.record(actor, "UPDATE_BOOK", "Book", id.toString(), null, "Updated details for: " + book.getTitle());

        return getBookDetails(id);
    }

    @Transactional
    public List<BookCopyResponse> addCopies(Long bookId, AddCopiesRequest request, String actor) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        Library defaultLib = libraryRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Default Library not found"));

        String prefix = (request.barcodePrefix() != null && !request.barcodePrefix().isBlank())
                ? request.barcodePrefix().toUpperCase()
                : "CPY-" + bookId;

        List<BookCopyResponse> createdCopies = new ArrayList<>();
        for (int i = 0; i < request.count(); i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setLibrary(defaultLib);
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setAcquisitionDate(LocalDate.now());
            copy.setBarcode(prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            BookCopy saved = bookCopyRepository.save(copy);
            createdCopies.add(BookCopyResponse.from(saved));
        }

        auditService.record(actor, "ADD_COPIES", "Book", bookId.toString(), null, "Added " + request.count() + " copies");
        return createdCopies;
    }

    @Transactional(readOnly = true)
    public List<BookCopyResponse> getCopiesForBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book", bookId);
        }
        return bookCopyRepository.findByBookId(bookId).stream()
                .map(BookCopyResponse::from)
                .toList();
    }
}
