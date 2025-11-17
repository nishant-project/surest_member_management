package com.surest.surest_member_management.repository;

import com.surest.surest_member_management.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Member> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

    Page<Member> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

    Page<Member> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName, String lastName, Pageable pageable);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
