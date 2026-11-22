package com.librarymanagement.reservations.web;

import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.common.response.PagedResponse;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.reservations.domain.ReservationStatus;
import com.librarymanagement.reservations.dto.ReservationDTOs.*;
import com.librarymanagement.reservations.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Endpoints for managing book title holds, FIFO queue ordering, and reservation cancellations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Place a new FIFO reservation hold on a book title")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReservationResponse response = reservationService.createReservation(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Reservation created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve detailed information for a specific reservation record")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReservationResponse response = reservationService.getReservationById(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Reservation retrieved successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an active reservation and trigger queue re-allocation")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reservationService.cancelReservation(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled successfully", null));
    }

    @GetMapping
    @Operation(summary = "Search and list reservations with pagination and filtering")
    public ResponseEntity<ApiResponse<PagedResponse<ReservationResponse>>> getReservations(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            memberId = principal.getMemberId();
        }

        int boundedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("requestDate").descending());

        Page<ReservationResponse> pagedResult = reservationService.getReservations(memberId, status, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok("Reservations retrieved successfully", PagedResponse.from(pagedResult)));
    }
}
