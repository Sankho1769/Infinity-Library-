package com.librarymanagement.members.web;

import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.dto.CirculationDTOs.LoanResponse;
import com.librarymanagement.circulation.service.CirculationService;
import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.common.response.PagedResponse;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.fines.dto.FineDTOs.FineResponse;
import com.librarymanagement.fines.service.FineService;
import com.librarymanagement.members.domain.MemberStatus;
import com.librarymanagement.members.dto.MemberDTOs.*;
import com.librarymanagement.members.service.MemberService;
import com.librarymanagement.reservations.domain.ReservationStatus;
import com.librarymanagement.reservations.dto.ReservationDTOs.ReservationResponse;
import com.librarymanagement.reservations.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Endpoints for library member registration, status lifecycle, and profile audits")
public class MemberController {
    private final MemberService memberService;
    private final CirculationService circulationService;
    private final FineService fineService;
    private final ReservationService reservationService;

    public MemberController(
            MemberService memberService,
            CirculationService circulationService,
            FineService fineService,
            ReservationService reservationService
    ) {
        this.memberService = memberService;
        this.circulationService = circulationService;
        this.fineService = fineService;
        this.reservationService = reservationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Search library members with pagination and status filters (Staff Only)")
    public ResponseEntity<ApiResponse<PagedResponse<MemberResponse>>> searchMembers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MemberStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.searchMembers(query, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('MEMBER') and #id == principal.memberId)")
    @Operation(summary = "Retrieve member details (Subject to Object-Level Authorization)")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getMemberById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Register a new library member (Staff Only)")
    public ResponseEntity<ApiResponse<MemberResponse>> registerMember(
            @Valid @RequestBody RegisterMemberRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        MemberResponse response = memberService.registerMember(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Member registered successfully", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Update member profile details (Staff Only)")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(ApiResponse.ok("Member updated successfully", memberService.updateMember(id, request, actor)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Suspend borrowing privileges for a member (Staff Only)")
    public ResponseEntity<ApiResponse<MemberResponse>> suspendMember(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Policy violation or excessive overdue") String reason,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(ApiResponse.ok("Member suspended", memberService.setMemberStatus(id, MemberStatus.SUSPENDED, reason, actor)));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Reactivate a suspended member (Staff Only)")
    public ResponseEntity<ApiResponse<MemberResponse>> reactivateMember(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(ApiResponse.ok("Member reactivated", memberService.setMemberStatus(id, MemberStatus.ACTIVE, "Reactivation authorized", actor)));
    }

    @GetMapping("/{id}/loans")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('MEMBER') and #id == principal.memberId)")
    @Operation(summary = "Retrieve borrowing loan history for a specific member (BOLA protected)")
    public ResponseEntity<ApiResponse<PagedResponse<LoanResponse>>> getMemberLoans(
            @PathVariable Long id,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("issueDate").descending());
        return ResponseEntity.ok(ApiResponse.ok("Member loans retrieved", PagedResponse.from(circulationService.getLoans(id, status, pageRequest))));
    }

    @GetMapping("/{id}/fines")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('MEMBER') and #id == principal.memberId)")
    @Operation(summary = "Retrieve fine history and outstanding penalties for a specific member (BOLA protected)")
    public ResponseEntity<ApiResponse<PagedResponse<FineResponse>>> getMemberFines(
            @PathVariable Long id,
            @RequestParam(required = false) FineStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("accrualDate").descending());
        return ResponseEntity.ok(ApiResponse.ok("Member fines retrieved", PagedResponse.from(fineService.getFines(id, status, pageRequest))));
    }

    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('MEMBER') and #id == principal.memberId)")
    @Operation(summary = "Retrieve active and historical reservations for a specific member (BOLA protected)")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationResponse>>> getMemberReservations(
            @PathVariable Long id,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("requestDate").descending());
        return ResponseEntity.ok(ApiResponse.ok("Member reservations retrieved", PagedResponse.from(reservationService.getReservations(id, status, pageRequest))));
    }
}
