package com.librarymanagement.circulation.web;

import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.dto.CirculationDTOs.*;
import com.librarymanagement.circulation.service.CirculationService;
import com.librarymanagement.common.exception.ErrorCode;
import com.librarymanagement.common.exception.ForbiddenException;
import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.common.response.PagedResponse;
import com.librarymanagement.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Circulation & Loans", description = "Endpoints for book issuing, returns, overdue tracking, and lending history")
public class LoanController {
    private final CirculationService circulationService;

    public LoanController(CirculationService circulationService) {
        this.circulationService = circulationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Issue a book copy to a member with pessimistic locking and idempotency protection")
    public ResponseEntity<ApiResponse<LoanResponse>> issueBook(
            @Valid @RequestBody IssueBookRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String actor = (principal != null) ? principal.getUsername() : "SYSTEM";
        LoanResponse response = circulationService.issueBook(request, idempotencyKey, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Book issued successfully", response));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Process return of an issued book copy, calculate fines, and allocate to pending reservations")
    public ResponseEntity<ApiResponse<ReturnBookResponse>> returnBookById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String actor = (principal != null) ? principal.getUsername() : "SYSTEM";
        ReturnBookRequest request = new ReturnBookRequest(id, null);
        ReturnBookResponse response = circulationService.returnBook(request, actor);
        return ResponseEntity.ok(ApiResponse.ok("Book returned successfully", response));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Process return of a book by barcode or loan ID")
    public ResponseEntity<ApiResponse<ReturnBookResponse>> returnBook(
            @Valid @RequestBody ReturnBookRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String actor = (principal != null) ? principal.getUsername() : "SYSTEM";
        ReturnBookResponse response = circulationService.returnBook(request, actor);
        return ResponseEntity.ok(ApiResponse.ok("Book returned successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve detailed information for a specific loan record")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LoanResponse loan = circulationService.getLoanById(id);

        // Object-Level Authorization Check (BOLA / IDOR protection)
        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            if (!loan.memberId().equals(principal.getMemberId())) {
                throw new ForbiddenException(ErrorCode.ACCESS_DENIED, "Access denied: cannot access another member's loan records");
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("Loan retrieved successfully", loan));
    }

    @GetMapping
    @Operation(summary = "Search and list loan records with bounded pagination and filtering")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponse>>> getLoans(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // Object-Level Authorization Check: Members can ONLY query their own loans
        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            memberId = principal.getMemberId();
        }

        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("issueDate").descending());

        Page<LoanResponse> loansPage = circulationService.getLoans(memberId, status, pageRequest);
        PagedResponse<LoanResponse> response = PagedResponse.from(loansPage);
        return ResponseEntity.ok(ApiResponse.ok("Loans retrieved successfully", response));
    }
}
