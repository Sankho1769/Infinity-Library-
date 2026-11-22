package com.librarymanagement.catalog.dto;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CatalogDTOs {

    public record CreateBookRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 500, message = "Title cannot exceed 500 characters")
            String title,

            @NotBlank(message = "ISBN is required")
            @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$|^(\\d{1,5}-\\d{1,7}-\\d{1,7}-[\\dX])$",
                     message = "Invalid ISBN format")
            String isbn,

            @NotBlank(message = "Author is required")
            String author,

            String publisher,
            Integer publicationYear,

            @NotBlank(message = "Genre is required")
            String genre,

            String language,
            String description,

            @Min(value = 1, message = "Initial copies must be at least 1")
            int initialCopies
    ) {}

    public record UpdateBookRequest(
            @NotBlank(message = "Title is required")
            String title,

            @NotBlank(message = "Author is required")
            String author,

            String publisher,
            Integer publicationYear,

            @NotBlank(message = "Genre is required")
            String genre,

            String language,
            String description
    ) {}

    public record AddCopiesRequest(
            @Min(value = 1, message = "Must add at least 1 copy")
            int count,

            String barcodePrefix
    ) {}

    public record BookResponse(
            Long id,
            String title,
            String isbn,
            String author,
            String publisher,
            Integer publicationYear,
            String genre,
            String language,
            String description,
            boolean active,
            long totalCopies,
            long availableCopies,
            long issuedCopies,
            long reservedCopies
    ) {
        public static BookResponse of(Book book, long total, long available, long issued, long reserved) {
            return new BookResponse(
                    book.getId(),
                    book.getTitle(),
                    book.getIsbn(),
                    book.getAuthor(),
                    book.getPublisher(),
                    book.getPublicationYear(),
                    book.getGenre(),
                    book.getLanguage(),
                    book.getDescription(),
                    book.isActive(),
                    total,
                    available,
                    issued,
                    reserved
            );
        }
    }

    public record BookCopyResponse(
            Long id,
            Long bookId,
            String bookTitle,
            String barcode,
            BookCopyStatus status,
            LocalDate acquisitionDate
    ) {
        public static BookCopyResponse from(BookCopy copy) {
            return new BookCopyResponse(
                    copy.getId(),
                    copy.getBook().getId(),
                    copy.getBook().getTitle(),
                    copy.getBarcode(),
                    copy.getStatus(),
                    copy.getAcquisitionDate()
            );
        }
    }
}
