package com.apishield.userservice.security;

import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            if (jwtService.validateToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UUID userId = jwtService.extractUserId(token);
                    Role role = jwtService.extractRole(token);

                    // Step 8.5 Security Enforcement:
                    // Validate that the user exists and their account status is strictly ACTIVE.
                    // This immediately revokes authenticated access for suspended or deactivated accounts
                    // without needing to wait for access token expiration.
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                            log.warn("Authenticated request blocked: User {} account status is {}", userId, user.getAccountStatus());
                            String reason = user.getAccountStatus() == AccountStatus.SUSPENDED ?
                                    "Account has been suspended. Please contact support." :
                                    "Account is inactive. Please activate your account or verify your email.";
                            sendForbiddenResponse(response, reason, request.getRequestURI());
                            return;
                        }
                    }

                    // Use current role from database to prevent stale token privilege issues
                    Role liveRole = userOpt.map(User::getRole).orElse(role);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + liveRole.name());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            Collections.singletonList(authority)
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Populated security context for user ID: {} with role: {}", userId, liveRole);
                } catch (Exception ex) {
                    log.warn("Failed to process authenticated token: {}", ex.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format("{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), message, path);
        response.getWriter().write(body);
    }
}
