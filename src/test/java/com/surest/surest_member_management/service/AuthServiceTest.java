package com.surest.surest_member_management.service;

import com.surest.surest_member_management.config.JwtUtil;
import com.surest.surest_member_management.dto.AuthRequest;
import com.surest.surest_member_management.dto.AuthResponse;
import com.surest.surest_member_management.exception.NotFoundException;
import com.surest.surest_member_management.exception.UnauthorizedException;
import org.springframework.security.authentication.BadCredentialsException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loginSuccess() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");

        // Mock userDetailsService
        User userDetails = new User(
                "admin",
                "Admin@123",
                List.of((GrantedAuthority) () -> "ROLE_ADMIN")  // List ensures stream() works
        );
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

        // Mock JWT generation
        when(jwtUtil.generateToken("admin", List.of("ROLE_ADMIN"))).thenReturn("dummy-jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertEquals("dummy-jwt-token", response.getToken());

        // Verify interactions
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("admin");
        verify(jwtUtil).generateToken("admin", List.of("ROLE_ADMIN"));
    }

    @Test
    void loginShouldThrowUnauthorizedForBadCredentials() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("wrongpass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void loginShouldThrowUnauthorizedForDisabledUser() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");

        doThrow(new DisabledException("User disabled"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("User account is disabled", ex.getMessage());
    }

    @Test
    void loginShouldThrowNotFoundForUsernameNotFound() {
        AuthRequest request = new AuthRequest();
        request.setUsername("unknown");
        request.setPassword("pass");

        doThrow(new UsernameNotFoundException("User not found"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        NotFoundException ex = assertThrows(NotFoundException.class, () -> authService.login(request));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void loginShouldThrowUnauthorizedForGenericException() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");

        doThrow(new RuntimeException("Unexpected error"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Authentication failed", ex.getMessage());
    }


}
