package com.taskflow.security;

import com.taskflow.repo.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Authenticates "Authorization: Bearer <jwt>". The User entity becomes the principal. */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwt.subject(header.substring(7));
                // Re-check the DB each request so deactivation takes effect immediately.
                users.findByEmailIgnoreCase(email).filter(u -> u.isActive() && u.isVerified()).ifPresent(u ->
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))));
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid/expired token -> request continues unauthenticated -> 401 on protected routes
            }
        }
        chain.doFilter(req, res);
    }
}
