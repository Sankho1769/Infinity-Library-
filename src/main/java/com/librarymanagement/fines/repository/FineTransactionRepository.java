package com.librarymanagement.fines.repository;

import com.librarymanagement.fines.domain.FineTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineTransactionRepository extends JpaRepository<FineTransaction, Long> {
    List<FineTransaction> findByFineIdOrderByTransactionDateDesc(Long fineId);
}
