package com.techup.travel_explorer_server.util;

import com.techup.travel_explorer_server.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    public Long getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No authentication token found. Please provide a valid JWT token in the Authorization header.");
        }
        
        String token = authHeader.substring(7);
        try {
            Long userId = jwtService.extractUserId(token);
            if (userId == null) {
                throw new RuntimeException("Invalid token: user ID not found in token");
            }
            return userId;
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired authentication token: " + e.getMessage());
        }
    }
    
    public String getCurrentUserEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No authentication token found");
        }
        
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        
        // Validate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.validateToken(token, userDetails)) {
            throw new RuntimeException("Invalid token");
        }
        
        return email;
    }
}

