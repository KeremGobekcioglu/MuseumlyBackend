package com.kg.museumly.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration means read this class at start up and register whatever @Bean methods
 * it finds.
 */
@Configuration
public class SecurityConfig
{
    /**
     * @Bean means “Spring, take the object this method returns and manage it.”
     * CSRF: off. CSRF protection defends against a malicious website making
     * the browser send a request with the user’s cookies automatically. Backend will serve
     * a mobile app so no problem.
     * Session is stateless, JWT will be used.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
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
                                .requestMatchers("/actuator/health", "/auth/**")
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
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(Customizer.withDefaults())
                );
        return http.build();
    }
}
