package com.example.fleet.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Request logging filter that logs HTTP method and path at INFO level.
 * Explicitly excludes request body to prevent password exposure.
 *
 * Requirements: 6.2
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Log HTTP method and path only — no request body to prevent password exposure
        logger.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }
}
