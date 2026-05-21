package org.example.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final String internalApiKey;

    public InternalApiKeyFilter(@Value("${internal.api-key}") String internalApiKey) {
        if (!StringUtils.hasText(internalApiKey)) {
            throw new IllegalStateException("internal.api-key must be configured");
        }
        this.internalApiKey = internalApiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (requiresInternalApiKey(request) && !apiKeyMatches(request.getHeader(HEADER_NAME))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid internal API key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresInternalApiKey(HttpServletRequest request) {
        String path = pathWithoutContext(request);
        return path.equals("/users") || path.startsWith("/users/");
    }

    private String pathWithoutContext(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private boolean apiKeyMatches(String candidate) {
        if (candidate == null) {
            return false;
        }

        byte[] expected = internalApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = candidate.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
