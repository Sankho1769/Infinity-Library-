package com.librarymanagement.reports.web;

import com.librarymanagement.common.response.ApiResponse;
import com.librarymanagement.reports.dto.ReportDTOs.*;
import com.librarymanagement.reports.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
@Tag(name = "Reports & Analytics", description = "Endpoints for database-derived operational metrics, inventory auditing, and financial reporting")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory")
    @Operation(summary = "Retrieve real-time physical inventory audit counts and copy distributions")
    public ResponseEntity<ApiResponse<InventoryReport>> getInventoryReport() {
        return ResponseEntity.ok(ApiResponse.ok("Inventory report generated successfully", reportService.getInventoryReport()));
    }

    @GetMapping("/circulation")
    @Operation(summary = "Retrieve circulation statistics including active, returned, and overdue loan counts")
    public ResponseEntity<ApiResponse<CirculationReport>> getCirculationReport() {
        return ResponseEntity.ok(ApiResponse.ok("Circulation report generated successfully", reportService.getCirculationReport()));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Retrieve itemized list of overdue loans with borrower and accrued penalty details")
    public ResponseEntity<ApiResponse<OverdueReport>> getOverdueReport() {
        return ResponseEntity.ok(ApiResponse.ok("Overdue loans report generated successfully", reportService.getOverdueReport()));
    }

    @GetMapping("/fines")
    @Operation(summary = "Retrieve aggregate fine accounting metrics including accrued, collected, and waived totals")
    public ResponseEntity<ApiResponse<FineSummaryReport>> getFineSummaryReport() {
        return ResponseEntity.ok(ApiResponse.ok("Fine financial summary generated successfully", reportService.getFineSummaryReport()));
    }
}
