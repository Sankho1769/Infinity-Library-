package com.librarymanagement.fines.web;

import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.common.response.PagedResponse;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.fines.dto.FineDTOs.*;
import com.librarymanagement.fines.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fines")
@Tag(name = "Fines & Penalties", description = "Endpoints for overdue fine inspection, payment recording, and authorized fee waivers")
public class FineController {
    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve fine assessment details and transaction history")
    public ResponseEntity<ApiResponse<FineResponse>> getFineById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FineResponse response = fineService.getFineById(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Fine retrieved successfully", response));
    }

    @GetMapping
    @Operation(summary = "List and filter fine records with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<FineResponse>>> getFines(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) FineStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            memberId = principal.getMemberId();
        }

        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("accrualDate").descending());

        Page<FineResponse> pagedResult = fineService.getFines(memberId, status, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok("Fines retrieved successfully", PagedResponse.from(pagedResult)));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Record an administrative fine payment against an outstanding balance")
    public ResponseEntity<ApiResponse<FineResponse>> payFine(
            @PathVariable Long id,
            @Valid @RequestBody PayFineRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String actor = (principal != null) ? principal.getUsername() : "STAFF";
        FineResponse response = fineService.payFine(id, request, actor);
        return ResponseEntity.ok(ApiResponse.ok("Payment recorded successfully", response));
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Waive an outstanding fine balance with mandatory audit justification")
    public ResponseEntity<ApiResponse<FineResponse>> waiveFine(
            @PathVariable Long id,
            @Valid @RequestBody WaiveFineRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String actor = (principal != null) ? principal.getUsername() : "STAFF";
        FineResponse response = fineService.waiveFine(id, request, actor);
        return ResponseEntity.ok(ApiResponse.ok("Fine waived successfully", response));
    }
}
