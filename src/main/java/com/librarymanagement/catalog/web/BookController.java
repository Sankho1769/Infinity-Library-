package com.librarymanagement.catalog.web;

import com.librarymanagement.catalog.dto.CatalogDTOs.*;
import com.librarymanagement.catalog.service.BookService;
import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books & Catalog", description = "Endpoints for bibliographic management, search, and physical copy inventory")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "Search and filter catalog books with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<BookResponse>>> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.searchBooks(query, genre, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed book information and live inventory counts")
    public ResponseEntity<ApiResponse<BookResponse>> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getBookDetails(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Register a new bibliographic title and physical copies (Staff Only)")
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Valid @RequestBody CreateBookRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        BookResponse response = bookService.registerBook(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Book registered successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Update bibliographic metadata (Staff Only)")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(ApiResponse.ok("Book updated successfully", bookService.updateBook(id, request, actor)));
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Add physical copies with barcodes to inventory (Staff Only)")
    public ResponseEntity<ApiResponse<List<BookCopyResponse>>> addCopies(
            @PathVariable Long id,
            @Valid @RequestBody AddCopiesRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        List<BookCopyResponse> copies = bookService.addCopies(id, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Copies added successfully", copies));
    }

    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "List all physical copies and statuses for a book (Staff Only)")
    public ResponseEntity<ApiResponse<List<BookCopyResponse>>> getCopies(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getCopiesForBook(id)));
    }
}
