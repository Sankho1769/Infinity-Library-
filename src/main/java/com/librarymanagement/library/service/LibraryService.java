package com.librarymanagement.library.service;

import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.dto.LibraryResponse;
import com.librarymanagement.library.dto.UpdateLibraryRequest;
import com.librarymanagement.library.repository.LibraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LibraryService {
    private final LibraryRepository libraryRepository;

    public LibraryService(LibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    @Transactional(readOnly = true)
    public Library getDefaultLibrary() {
        return libraryRepository.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Default Library not configured"));
    }

    @Transactional(readOnly = true)
    public LibraryResponse getLibraryDetails(Long id) {
        Library lib = libraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library", id));
        return LibraryResponse.from(lib);
    }

    @Transactional
    public LibraryResponse updateLibrary(Long id, UpdateLibraryRequest request) {
        Library lib = libraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library", id));

        lib.setName(request.name());
        if (request.address() != null) lib.setAddress(request.address());
        if (request.email() != null) lib.setEmail(request.email());
        if (request.phone() != null) lib.setPhone(request.phone());
        lib.setLoanDurationDays(request.loanDurationDays());
        lib.setMaxLoansPerMember(request.maxLoansPerMember());
        lib.setDailyFineRate(request.dailyFineRate());
        lib.setMaxFinePerLoan(request.maxFinePerLoan());
        lib.setReservationHoldHours(request.reservationHoldHours());
        lib.setUpdatedAt(Instant.now());

        return LibraryResponse.from(libraryRepository.save(lib));
    }
}
