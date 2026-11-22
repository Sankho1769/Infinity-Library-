package com.librarymanagement.auth.repository;

import com.librarymanagement.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.member WHERE u.username = :username")
    Optional<UserAccount> findByUsernameWithMember(@Param("username") String username);

    Optional<UserAccount> findByMemberId(Long memberId);
}
