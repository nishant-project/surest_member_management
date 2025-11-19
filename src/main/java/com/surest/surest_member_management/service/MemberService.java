
package com.surest.surest_member_management.service;


import com.surest.surest_member_management.dto.MemberRequest;
import com.surest.surest_member_management.dto.MemberResponse;
import com.surest.surest_member_management.entity.Member;
import com.surest.surest_member_management.exception.NotFoundException;
import com.surest.surest_member_management.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        log.info("Attempting to create member with email: {}", request.getEmail());

        if (memberRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        Member member = Member.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .build();

        Member saved = memberRepository.saveAndFlush(member);

        log.debug("Member created successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }



    public Page<MemberResponse> getAllMembers(String firstName, String lastName, Pageable pageable) {
        log.info("Fetching members with filters - firstName: {}, lastName: {}", firstName, lastName);

        Page<Member> memberPage;

        if (firstName != null && lastName != null) {
            memberPage = memberRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName, pageable);
        } else if (firstName != null) {
            memberPage = memberRepository.findByFirstNameContainingIgnoreCase(firstName, pageable);
        } else if (lastName != null) {
            memberPage = memberRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
        } else {
            memberPage = memberRepository.findAll(pageable);
        }

        return memberPage.map(this::mapToResponse);
    }



    @Cacheable(value = "members", key = "#id")
    public MemberResponse getMemberById(UUID id) {
        log.info("Fetching member by ID: {}", id);
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        return mapToResponse(member);
    }



    @Transactional
    @CachePut(value = "members", key = "#id")
    public MemberResponse updateMember(UUID id, MemberRequest request) {
        log.info("Updating member with ID: {}", id);

        Member existing = memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        String existingEmail = existing.getEmail();
        String newEmail = request.getEmail();

        if (!Objects.equals(existingEmail, newEmail) &&
                memberRepository.existsByEmailAndIdNot(newEmail, id)) {
            log.warn("Duplicate email detected during update: {}", newEmail);
            throw new IllegalArgumentException("Email already exists");
        }

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setEmail(newEmail);

        Member updated = memberRepository.save(existing);
        log.debug("Member updated successfully with ID: {}", updated.getId());

        return mapToResponse(updated);
    }



    @Transactional
    @CacheEvict(value = "members", key = "#id")
    public void deleteMember(UUID id) {
        log.warn("Deleting member with ID: {}", id);
        if (!memberRepository.existsById(id)) {
            log.error("Delete failed - member not found with ID: {}", id);
            throw new NotFoundException("Member not found");
        }
        memberRepository.deleteById(id);
        log.info("Member deleted successfully with ID: {}", id);
    }



    private MemberResponse mapToResponse(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .dateOfBirth(member.getDateOfBirth())
                .email(member.getEmail())
                .build();
    }
}
