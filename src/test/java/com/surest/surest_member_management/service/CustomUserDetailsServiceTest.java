package com.surest.surest_member_management.service;

import com.surest.surest_member_management.entity.Role;
import com.surest.surest_member_management.entity.User;
import com.surest.surest_member_management.exception.NotFoundException;
import com.surest.surest_member_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------- Success Test ----------------------
    @Test
    void loadUserByUsernameSuccess() {
        Role role = new Role();
        role.setName("ROLE_ADMIN");

        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("Admin@123");
        user.setRole(role);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");
        assertEquals("admin", userDetails.getUsername());
        assertEquals("Admin@123", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(userRepository).findByUsername("admin");
    }

    @Test
    void loadUserByUsernameUserNotFoundThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                userDetailsService.loadUserByUsername("unknown")
        );
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByUsername("unknown");
    }

}
