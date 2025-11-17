
package com.surest.surest_member_management.controller;

import com.surest.surest_member_management.dto.MemberRequest;
import com.surest.surest_member_management.dto.MemberResponse;
import com.surest.surest_member_management.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;


    // Create a new member (Admin only)

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberRequest request) {
        log.info("Creating new member: {}", request.getFirstName());
        MemberResponse response = memberService.createMember(request);
        log.debug("Member created with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // Retrieve paginated list of members with optional filtering and sorting.Accessible to both USER and ADMIN roles.

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Page<MemberResponse>> getAllMembers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortBy = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1) ? Sort.Direction.fromString(sortParts[1]) : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        log.info("Fetching members (page={}, size={}, sortBy={}, direction={})", page, size, sortBy, direction);

        Page<MemberResponse> members = memberService.getAllMembers(firstName, lastName, pageable);
        return ResponseEntity.ok(members);
    }

    // Get member by ID. Accessible to USER and ADMIN roles.

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable UUID id) {
        log.info("Fetching member with ID: {}", id);
        MemberResponse member = memberService.getMemberById(id);
        return ResponseEntity.ok(member);
    }


    // Update existing member details (Admin only)

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable UUID id,
            @Valid @RequestBody MemberRequest request) {
        log.info("Updating member with ID: {}", id);
        MemberResponse updatedMember = memberService.updateMember(id, request);
        log.debug("Member updated successfully: {}", updatedMember);
        return ResponseEntity.ok(updatedMember);
    }

    // Delete a member by ID (Admin only)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMember(@PathVariable UUID id) {
        log.warn("Deleting member with ID: {}", id);
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}