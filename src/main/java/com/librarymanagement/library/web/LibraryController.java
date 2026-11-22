package com.librarymanagement.library.web;

import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.library.dto.LibraryResponse;
import com.librarymanagement.library.dto.UpdateLibraryRequest;
import com.librarymanagement.library.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/libraries")
@Tag(name = "Library Management", description = "Endpoints for institutional configuration and lending policies")
public class LibraryController {
    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get library operational configuration by ID")
    public ResponseEntity<ApiResponse<LibraryResponse>> getLibrary(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(libraryService.getLibraryDetails(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update library policies (Admin Only)")
    public ResponseEntity<ApiResponse<LibraryResponse>> updateLibrary(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLibraryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Library configuration updated successfully", libraryService.updateLibrary(id, request)));
    }
}
