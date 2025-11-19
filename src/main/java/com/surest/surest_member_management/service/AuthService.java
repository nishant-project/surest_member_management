package com.surest.surest_member_management.service;

import com.surest.surest_member_management.config.JwtUtil;
import com.surest.surest_member_management.dto.AuthRequest;
import com.surest.surest_member_management.dto.AuthResponse;
import com.surest.surest_member_management.exception.NotFoundException;
import com.surest.surest_member_management.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            var userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            var token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            return new AuthResponse(token);

        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid username or password", ex);

        } catch (DisabledException ex) {
            throw new UnauthorizedException("User account is disabled", ex);

        } catch (AuthenticationException ex) {

            throw new NotFoundException("User not found");

        } catch (Exception ex) {

            throw new UnauthorizedException("Authentication failed", ex);
        }
    }
}
