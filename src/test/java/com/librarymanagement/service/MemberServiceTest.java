package com.librarymanagement.service;

import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.ConflictException;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import com.librarymanagement.members.dto.MemberDTOs.*;
import com.librarymanagement.members.repository.MemberRepository;
import com.librarymanagement.members.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private AuditService auditService;

    private MemberService memberService;
    private Member member;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, auditService);

        member = new Member();
        member.setId(1L);
        member.setName("Marcus Vance");
        member.setEmail("marcus.v@test.org");
        member.setMembershipNumber("MEM-2026-0002");
        member.setStatus(MemberStatus.ACTIVE);
        member.setMembershipStartDate(LocalDate.now());
        member.setMembershipExpiryDate(LocalDate.now().plusYears(1));
    }

    @Test
    @DisplayName("Register new member successfully with auto-generated membership number")
    void testRegisterMemberSuccess() {
        when(memberRepository.existsByEmail("new.member@test.org")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        RegisterMemberRequest request = new RegisterMemberRequest(
                "New Member",
                "new.member@test.org",
                "+1-555-0199",
                12
        );

        MemberResponse response = memberService.registerMember(request, "librarian");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("New Member");
        assertThat(response.membershipNumber()).startsWith("MEM-");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("Register member with existing email throws ConflictException")
    void testRegisterDuplicateEmailThrowsConflict() {
        when(memberRepository.existsByEmail("marcus.v@test.org")).thenReturn(true);

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Marcus Vance",
                "marcus.v@test.org",
                null,
                12
        );

        assertThatThrownBy(() -> memberService.registerMember(request, "librarian"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Suspend member privileges successfully")
    void testSuspendMember() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.setMemberStatus(1L, MemberStatus.SUSPENDED, "Overdue penalty", "admin");

        assertThat(response.status()).isEqualTo(MemberStatus.SUSPENDED);
        verify(auditService).record(eq("admin"), eq("CHANGE_MEMBER_STATUS"), eq("Member"), eq("1"), isNull(), anyString());
    }
}
