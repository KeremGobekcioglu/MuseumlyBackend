package com.kg.museumly.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Called when a request is not authenticated (missing, invalid or expired token).
 * Security rejections happen in the filter chain, before any controller runs, so
 * GlobalExceptionHandler would never see them. This forwards the error to it so
 * 401 responses use the same ProblemDetail format as every other error.
 * <p>
 * The name is deliberately unusual: Spring already has a DelegatingAuthenticationEntryPoint.
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    // "handlerExceptionResolver" is the main resolver, the one that knows about @RestControllerAdvice
    public ProblemDetailAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        // handler is null: the error happened before any controller was chosen
        resolver.resolveException(request, response, null, authException);
    }
}