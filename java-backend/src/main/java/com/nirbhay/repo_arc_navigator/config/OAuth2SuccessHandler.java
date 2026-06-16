package com.nirbhay.repo_arc_navigator.config;

import com.nirbhay.repo_arc_navigator.entity.UserEntity;
import com.nirbhay.repo_arc_navigator.repository.UserJpaRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Called by Spring Security after a successful Google or GitHub OAuth login.
 * Creates or updates the user record, generates a 30-min JWT, and redirects
 * back to the React frontend with the token embedded in the URL fragment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserJpaRepo   userRepo;
    private final JwtUtil       jwtUtil;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // Extract email
        String email    = getAttribute(oauthUser, "email");
        String name     = getAttribute(oauthUser, "name");
        if (email == null) {
            log.error("[OAuth2] No email returned from provider — cannot create user");
            response.sendRedirect(frontendUrl + "/#error=no_email");
            return;
        }

        // Detect provider from registration ID stored in the auth details
        String provider = "unknown";
        try {
            var details = authentication.getDetails();
            if (details != null) {
                String str = details.toString();
                if (str.contains("google")) provider = "google";
            }
        } catch (Exception ignored) {}

        // Create user if first login
        final String finalName    = name != null ? name : email;
        final String finalProvider = provider;
        UserEntity user = userRepo.findByEmail(email).orElseGet(() -> UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .username(finalName)
                .provider(finalProvider)
                .build());

        user.setUsername(finalName);
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("[OAuth2] Login success: {} ({})", email, provider);

        // Redirect back to React with token in URL fragment (never in query string — not logged by servers)
        response.sendRedirect(frontendUrl + "/#token=" + token + "&username=" + java.net.URLEncoder.encode(finalName, "UTF-8"));
    }

    private String getAttribute(OAuth2User user, String key) {
        Object val = user.getAttributes().get(key);
        return val != null ? val.toString() : null;
    }
}
