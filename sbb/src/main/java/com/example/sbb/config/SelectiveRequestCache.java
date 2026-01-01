package com.example.sbb.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.util.AntPathMatcher;

/**
 * Save only real page navigations (HTML) and skip API/AJAX calls
 * so login redirect does not point to API endpoints.
 */
public class SelectiveRequestCache extends HttpSessionRequestCache {
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        boolean isApi = matcher.match("/api/**", uri);
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
        if (isApi || isAjax) {
            return; // do not record API/XHR to avoid redirect like /api/notifications/due
        }
        super.saveRequest(request, response);
    }
}
