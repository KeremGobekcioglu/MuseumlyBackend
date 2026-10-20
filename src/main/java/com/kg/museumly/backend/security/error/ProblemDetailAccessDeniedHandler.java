package com.kg.museumly.backend.security.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Called when a request is authenticated but not allowed (403). Forwards the error to
 * GlobalExceptionHandler so it uses the same ProblemDetail format as every other error.
 */
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public ProblemDetailAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        resolver.resolveException(request, response, null, accessDeniedException);
    }
}