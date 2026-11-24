package com.kg.museumly.backend.security;

import com.kg.museumly.backend.security.error.ProblemDetailAccessDeniedHandler;
import com.kg.museumly.backend.security.error.ProblemDetailAuthenticationEntryPoint;
import com.kg.museumly.backend.security.logging.RequestLoggingFilter;
import com.kg.museumly.backend.security.logging.UserMdcFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration means read this class at start up and register whatever @Bean methods
 * it finds.
 */
@Configuration
public class SecurityConfig
{
    // Anyone may call these, and the token is not even read on them.
    private static final String[] PUBLIC_PATHS = {"/actuator/health", "/auth/**", "/met/**"};
    /**
     * @Bean means “Spring, take the object this method returns and manage it.”
     * CSRF: off. CSRF protection defends against a malicious website making
     * the browser send a request with the user’s cookies automatically. Backend will serve
     * a mobile app so no problem.
     * Session is stateless, JWT will be used.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception
    {
        http
                .csrf(csrf -> csrf.disable())
                /**
                 * tells Spring “never create an HttpSession for any request.” Without this,
                 * Spring Security would create one on first contact
                 * and expect a cookie back, which a stateless JWT API doesn’t use.
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(PUBLIC_PATHS)
                                /**
                                 * .requestMatchers("/actuator/health", "/auth/**").permitAll()
                                 * any request matching either of these paths needs no authentication
                                 */
                                .permitAll()
                                /**
                                 * .anyRequest().authenticated() — the catch-all: anything not matched above requires an authenticated request.
                                 */
                                .anyRequest().authenticated()
                )
                /**
                 * Two different filters can reject a request, so the handlers are registered twice:
                 * exceptionHandling covers "no token" (stopped by the authenticated() rule), and
                 * oauth2ResourceServer covers "bad/expired token" (rejected by the Bearer filter itself).
                 * Both forward to GlobalExceptionHandler so errors share one JSON format.
                 */
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(publicPathsIgnoreToken())
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(new RequestLoggingFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new UserMdcFilter(), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
    // Public path → null (no token, anonymous), so a bad or expired token can't cause a 401.
    // Anywhere else → Spring's default: the Authorization: Bearer header.
    private BearerTokenResolver publicPathsIgnoreToken()
    {
        DefaultBearerTokenResolver defaultBearerTokenResolver = new DefaultBearerTokenResolver();
        List<RequestMatcher> publicMatchers = new ArrayList<>();
        for (String path : PUBLIC_PATHS) {
            publicMatchers.add(PathPatternRequestMatcher.withDefaults().matcher(path));
        }
        return request -> {
            for(RequestMatcher matcher : publicMatchers)
            {
                if(matcher.matches(request))
                {
                    return null;
                }
            }
            return defaultBearerTokenResolver.resolve(request);
        };
    }
}
