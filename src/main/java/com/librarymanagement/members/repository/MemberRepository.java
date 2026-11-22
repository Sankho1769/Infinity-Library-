package com.librarymanagement.members.repository;

import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMembershipNumber(String membershipNumber);
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT m FROM Member m WHERE " +
           "(:query IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(m.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(m.membershipNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR m.status = :status)")
    Page<Member> searchMembers(@Param("query") String query, @Param("status") MemberStatus status, Pageable pageable);
}
