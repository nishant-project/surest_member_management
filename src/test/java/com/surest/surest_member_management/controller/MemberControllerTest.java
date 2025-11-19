package com.surest.surest_member_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surest.surest_member_management.config.JwtUtil;
import com.surest.surest_member_management.dto.MemberRequest;
import com.surest.surest_member_management.dto.MemberResponse;
import com.surest.surest_member_management.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private MemberResponse sampleMember;

    @BeforeEach
    void setup() {
        sampleMember = MemberResponse.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.com")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMemberAsAdminShouldReturnCreatedMember() throws Exception {
        MemberRequest request = new MemberRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setEmail("john.doe@example.com");

        Mockito.when(memberService.createMember(any(MemberRequest.class)))
                .thenReturn(sampleMember);

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sampleMember.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listMembersAsUserShouldReturnPage() throws Exception {
        Page<MemberResponse> page = new PageImpl<>(List.of(sampleMember), PageRequest.of(0,20), 1);
        Mockito.when(memberService.getAllMembers(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleMember.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByIdAsUserShouldReturnMember() throws Exception {
        Mockito.when(memberService.getMemberById(any(UUID.class))).thenReturn(sampleMember);

        mockMvc.perform(get("/api/v1/members/{id}", sampleMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMemberAsAdminShouldReturnUpdatedMember() throws Exception {
        MemberRequest req = new MemberRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setDateOfBirth(LocalDate.of(1992, 2, 2));
        req.setEmail("jane.doe@example.com");

        MemberResponse updated = MemberResponse.builder()
                .id(sampleMember.getId())
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .email("jane.doe@example.com")
                .build();

        Mockito.when(memberService.updateMember(eq(sampleMember.getId()), any(MemberRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/members/{id}", sampleMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMemberAsAdminShouldReturnNoContent() throws Exception {
        Mockito.doNothing().when(memberService).deleteMember(any(UUID.class));

        mockMvc.perform(delete("/api/v1/members/{id}", sampleMember.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listMembersSortSingleFieldShouldUseAscByDefault() throws Exception {
        Page<MemberResponse> page = new PageImpl<>(List.of(sampleMember));
        Mockito.when(memberService.getAllMembers(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/members")
                        .param("sort", "firstName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleMember.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMemberInvalidRequestShouldReturnBadRequest() throws Exception {
        // send missing required fields (empty JSON)
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/members"));
       }

}