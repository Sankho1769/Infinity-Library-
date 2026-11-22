package com.librarymanagement.members.service;

import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.ConflictException;
import com.librarymanagement.common.exception.ErrorCode;
import com.librarymanagement.common.exception.ResourceNotFoundException;
import com.librarymanagement.common.response.PagedResponse;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import com.librarymanagement.members.dto.MemberDTOs.*;
import com.librarymanagement.members.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    public MemberService(MemberRepository memberRepository, AuditService auditService) {
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MemberResponse registerMember(RegisterMemberRequest request, String actor) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorCode.INVALID_OPERATION, "Member with email " + request.email() + " already exists");
        }

        int months = (request.membershipDurationMonths() != null && request.membershipDurationMonths() > 0)
                ? request.membershipDurationMonths()
                : 12;

        LocalDate now = LocalDate.now();
        Member member = new Member();
        member.setName(request.name());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setStatus(MemberStatus.ACTIVE);
        member.setMembershipStartDate(now);
        member.setMembershipExpiryDate(now.plusMonths(months));
        member.setMembershipNumber("MEM-" + now.getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        Member saved = memberRepository.save(member);
        auditService.record(actor, "REGISTER_MEMBER", "Member", saved.getId().toString(), null, "Registered member: " + saved.getName());

        return MemberResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        return MemberResponse.from(member);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MemberResponse> searchMembers(String query, MemberStatus status, Pageable pageable) {
        Page<Member> page = memberRepository.searchMembers(query, status, pageable);
        return PagedResponse.from(page.map(MemberResponse::from));
    }

    @Transactional
    public MemberResponse updateMember(Long id, UpdateMemberRequest request, String actor) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));

        member.setName(request.name());
        if (request.email() != null && !request.email().equalsIgnoreCase(member.getEmail())) {
            if (memberRepository.existsByEmail(request.email())) {
                throw new ConflictException(ErrorCode.INVALID_OPERATION, "Email " + request.email() + " is already registered");
            }
            member.setEmail(request.email());
        }
        if (request.phone() != null) member.setPhone(request.phone());
        if (request.status() != null) member.setStatus(request.status());
        member.setUpdatedAt(Instant.now());

        Member saved = memberRepository.save(member);
        auditService.record(actor, "UPDATE_MEMBER", "Member", id.toString(), null, "Updated member profile");

        return MemberResponse.from(saved);
    }

    @Transactional
    public MemberResponse setMemberStatus(Long id, MemberStatus newStatus, String reason, String actor) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));

        member.setStatus(newStatus);
        member.setUpdatedAt(Instant.now());
        Member saved = memberRepository.save(member);

        auditService.record(actor, "CHANGE_MEMBER_STATUS", "Member", id.toString(), null, "Status changed to " + newStatus + ". Reason: " + reason);
        return MemberResponse.from(saved);
    }
}
