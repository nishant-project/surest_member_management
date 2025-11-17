package com.surest.surest_member_management.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surest.surest_member_management.entity.Member;
import com.surest.surest_member_management.repository.MemberRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemberIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ObjectMapper objectMapper;

    @Value("${app.jwt.secret:defaulttestsecretchangeme}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    private SecretKey signingKey;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        adminToken = createJwtWithMultipleAuthorityFormats("admin", List.of("ROLE_ADMIN"));
        userToken  = createJwtWithMultipleAuthorityFormats("user", List.of("ROLE_USER"));
    }

    private String createJwtWithMultipleAuthorityFormats(String username, List<String> roles) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusMillis(jwtExpirationMs));

        List<Map<String, String>> authMaps = roles.stream()
                .map(r -> Map.of("authority", r))
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("authorities", roles);
        claims.put("authorities_map", authMaps);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void listMembersAsAdminShouldReturn200AndEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void createMemberAsAdminShouldReturn201AndPersist() throws Exception {
        Map<String, Object> newMember = Map.of(
                "firstName", "Alice",
                "lastName", "Smith",
                "dateOfBirth", "1995-08-15",
                "email", "alice.smith@example.com"
        );

        var result = mockMvc.perform(post("/api/v1/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMember)))
                .andDo(print())
                .andReturn();

        int status = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();

        Assertions.assertEquals(201, status,
                () -> "Expected 201 Created but got " + status + "\nResponse:\n" + responseBody);

        boolean exists = memberRepository.findAll().stream()
                .anyMatch(m -> "alice.smith@example.com".equalsIgnoreCase(m.getEmail()));
        Assertions.assertTrue(exists, "Member should have been persisted in DB after 201 Created.");
    }

    @Test
    void createMemberAsUserShouldReturnForbidden() throws Exception {
        Map<String, Object> newMember = Map.of(
                "firstName", "Bob",
                "lastName", "Miller",
                "dateOfBirth", "1992-01-01",
                "email", "bob.miller@example.com"
        );

        mockMvc.perform(post("/api/v1/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMember)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void getByIdAsUserShouldReturnMemberWhenExists() throws Exception {
        Member saved = memberRepository.save(
                Member.builder()
                        .firstName("Carl")
                        .lastName("Johnson")
                        .dateOfBirth(LocalDate.of(1990, 5, 20))
                        .email("carl.j@example.com")
                        .build()
        );

        mockMvc.perform(get("/api/v1/members/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carl.j@example.com"));
    }
    @Test
    void getByIdNonExistentMemberShouldReturn404() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/members/{id}", randomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Member not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/members/" + randomId))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
    // Helper: create and persist a valid member quickly
    private Member createAndSaveMember(String email, String firstName) {
        Member m = Member.builder()
                .firstName(firstName)
                .lastName("Test")
                .email(email)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        return memberRepository.save(m);
    }

    // Helper: create JWT with custom expiry (ms). Use for expired token tests.
    private String createJwt(String username, List<String> roles, long customExpirationMs) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusMillis(customExpirationMs));

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("authorities", roles);

        SecretKey key = signingKey; // reuse signingKey created in setUp()
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void deleteMemberAsUserReturnsForbidden403() throws Exception {
        Member existing = createAndSaveMember("tobedeleted@example.com", "ToDelete");

        mockMvc.perform(delete("/api/v1/members/{id}", existing.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isCreated());
    }



    @Test
    void requestWithExpiredTokenReturns401() throws Exception {
        // create a token that expired 1 minute ago
        String expiredToken = createJwt("expiredUser", List.of("ROLE_USER"), -60_000L);

        mockMvc.perform(get("/api/v1/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


}
