package com.surest.surest_member_management.service;

import com.surest.surest_member_management.dto.MemberRequest;
import com.surest.surest_member_management.dto.MemberResponse;
import com.surest.surest_member_management.entity.Member;
import com.surest.surest_member_management.exception.NotFoundException;
import com.surest.surest_member_management.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.*;


import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    private MemberRequest sampleRequest;
    private Member sampleMember;

    @BeforeEach
    void setUp() {
        sampleRequest = MemberRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        sampleMember = Member.builder()
                .id(UUID.randomUUID())
                .firstName(sampleRequest.getFirstName())
                .lastName(sampleRequest.getLastName())
                .email(sampleRequest.getEmail())
                .dateOfBirth(sampleRequest.getDateOfBirth())


                .build();
    }

    @Test
    void createMemberSuccess() {
        when(memberRepository.existsByEmail(sampleRequest.getEmail())).thenReturn(false);
        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(sampleMember);

        MemberResponse resp = memberService.createMember(sampleRequest);

        assertNotNull(resp);
        assertEquals(sampleMember.getId(), resp.getId());
        assertEquals("John", resp.getFirstName());
        assertEquals("Doe", resp.getLastName());
        assertEquals("john.doe@example.com", resp.getEmail());

        verify(memberRepository).existsByEmail(sampleRequest.getEmail());
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        Member captured = memberCaptor.getValue();
        assertEquals("John", captured.getFirstName());
        assertEquals("Doe", captured.getLastName());
        assertEquals("john.doe@example.com", captured.getEmail());
        assertEquals(sampleRequest.getDateOfBirth(), captured.getDateOfBirth());
    }

    @Test
    void createMemberEmailAlreadyExists_throws() {
        when(memberRepository.existsByEmail(sampleRequest.getEmail())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                memberService.createMember(sampleRequest)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(memberRepository).existsByEmail(sampleRequest.getEmail());
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void getByIdSuccess() {
        UUID id = sampleMember.getId();
        when(memberRepository.findById(id)).thenReturn(Optional.of(sampleMember));

        MemberResponse resp = memberService.getMemberById(id);

        assertNotNull(resp);
        assertEquals(id, resp.getId());
        assertEquals(sampleMember.getEmail(), resp.getEmail());
        verify(memberRepository).findById(id);
    }

    @Test
    void getByIdNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(memberRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> memberService.getMemberById(id));
        assertTrue(ex.getMessage().toLowerCase().contains("member"));
        verify(memberRepository).findById(id);
    }

    @Test
    void listMembersNoFiltersReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("firstName"));
        List<Member> members = Collections.singletonList(sampleMember);
        Page<Member> page = new PageImpl<>(members, pageable, members.size());

        when(memberRepository.findAll(pageable)).thenReturn(page);

        Page<MemberResponse> result = memberService.getAllMembers(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleMember.getEmail(), result.getContent().get(0).getEmail());
        verify(memberRepository).findAll(pageable);
    }

    @Test
    void listMembersFirstNameFilterCallsRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 5);
        List<Member> members = Collections.singletonList(sampleMember);
        Page<Member> page = new PageImpl<>(members, pageable, members.size());

        when(memberRepository.findByFirstNameContainingIgnoreCase("Jo", pageable)).thenReturn(page);

        Page<MemberResponse> result = memberService.getAllMembers("Jo", null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(memberRepository).findByFirstNameContainingIgnoreCase("Jo", pageable);
        verifyNoMoreInteractions(memberRepository);
    }
    @Test
    void listMembersFirstAndLastNameFilterCallsCombinedRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 5);
        List<Member> members = Collections.singletonList(sampleMember);
        Page<Member> page = new PageImpl<>(members, pageable, members.size());

        when(memberRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("Jo", "Do", pageable))
                .thenReturn(page);

        Page<MemberResponse> result = memberService.getAllMembers("Jo", "Do", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(sampleMember.getEmail(), result.getContent().get(0).getEmail());

        verify(memberRepository).findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("Jo", "Do", pageable);
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void listMembersLastNameFilterCallsRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 5);
        List<Member> members = Collections.singletonList(sampleMember);
        Page<Member> page = new PageImpl<>(members, pageable, members.size());

        when(memberRepository.findByLastNameContainingIgnoreCase("Doe", pageable)).thenReturn(page);

        Page<MemberResponse> result = memberService.getAllMembers(null, "Doe", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(sampleMember.getLastName(), result.getContent().get(0).getLastName());
        verify(memberRepository).findByLastNameContainingIgnoreCase("Doe", pageable);
        verifyNoMoreInteractions(memberRepository);
    }
    @Test
    void updateMemberSuccessEmailChanged() {
        UUID id = sampleMember.getId();
        MemberRequest updateReq = MemberRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .build();

        Member existing = Member.builder()
                .id(id)
                .firstName("Old")
                .lastName("Name")
                .email("old@example.com")
                .dateOfBirth(LocalDate.of(1970, 1, 1))
                .build();

        Member saved = Member.builder()
                .id(id)
                .firstName(updateReq.getFirstName())
                .lastName(updateReq.getLastName())
                .email(updateReq.getEmail())
                .dateOfBirth(updateReq.getDateOfBirth())


                .build();

        when(memberRepository.findById(id)).thenReturn(Optional.of(existing));
        when(memberRepository.existsByEmailAndIdNot(updateReq.getEmail(), id)).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(saved);

        MemberResponse resp = memberService.updateMember(id, updateReq);

        assertNotNull(resp);
        assertEquals(id, resp.getId());
        assertEquals("Jane", resp.getFirstName());
        assertEquals(updateReq.getEmail(), resp.getEmail());

        verify(memberRepository).findById(id);
        verify(memberRepository).existsByEmailAndIdNot(updateReq.getEmail(), id);
        verify(memberRepository).save(memberCaptor.capture());
        Member captured = memberCaptor.getValue();
        assertEquals("Jane", captured.getFirstName());
        assertEquals("Smith", captured.getLastName());
        assertEquals(updateReq.getEmail(), captured.getEmail());
    }

    @Test
    void updateMemberSuccessEmailUnchanged() {
        UUID id = sampleMember.getId();
        MemberRequest updateReq = MemberRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(sampleMember.getEmail()) // same email
                .dateOfBirth(sampleMember.getDateOfBirth())
                .build();

        when(memberRepository.findById(id)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenReturn(sampleMember);

        MemberResponse resp = memberService.updateMember(id, updateReq);

        assertEquals(sampleMember.getEmail(), resp.getEmail());
        verify(memberRepository, never()).existsByEmailAndIdNot(anyString(), any());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void updateMemberEmailConflictThrows() {
        UUID id = sampleMember.getId();
        MemberRequest updateReq = MemberRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("existing@example.com") // different email
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .build();

        when(memberRepository.findById(id)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.existsByEmailAndIdNot(updateReq.getEmail(), id)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberService.updateMember(id, updateReq));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(memberRepository).findById(id);
        verify(memberRepository).existsByEmailAndIdNot(updateReq.getEmail(), id);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void updateMemberNotFoundThrows() {
        UUID id = UUID.randomUUID();
        MemberRequest updateReq = MemberRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .build();

        when(memberRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> memberService.updateMember(id, updateReq));

        assertTrue(ex.getMessage().toLowerCase().contains("member"));
        verify(memberRepository).findById(id);
        verify(memberRepository, never()).existsByEmailAndIdNot(anyString(), any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void deleteSuccess() {
        UUID id = sampleMember.getId();
        when(memberRepository.existsById(id)).thenReturn(true);
        doNothing().when(memberRepository).deleteById(id);

        memberService.deleteMember(id);

        verify(memberRepository).existsById(id);
        verify(memberRepository).deleteById(id);
    }

    @Test
    void deleteNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(memberRepository.existsById(id)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> memberService.deleteMember(id));
        assertTrue(ex.getMessage().toLowerCase().contains("member"));

        verify(memberRepository).existsById(id);
        verify(memberRepository, never()).deleteById(any());
    }


}