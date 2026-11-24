package com.kg.museumly.backend;

// Same setup as AuthEdgeCasesIntegrationTest; adjust imports to match it.
import com.kg.museumly.backend.security.google.GoogleTokenVerificationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class PublicPathsSecurityIntegrationTest {
    private static final String MET_URL = "/met/artworks?department=11";

    @Autowired
    private MockMvc mockMvc;

    // Same mock as the other full-app tests, so Spring reuses their context
    // and nothing calls Google for real.
    @MockitoBean
    private GoogleTokenVerificationClient googleTokenVerificationClient;
    @Autowired
    private JwtEncoder jwtEncoder;

    // Signed with our key, but it expired a day ago.
    private String expiredToken() {
        Instant past = Instant.now().minus(Duration.ofDays(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("museumly-backend")
                .subject(UUID.randomUUID().toString())
                .issuedAt(past)
                .expiresAt(past.plus(Duration.ofDays(1)))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void met_noToken_is200() throws Exception {
        mockMvc.perform(get(MET_URL))
                .andExpect(status().isOk());
    }

    @Test
    void met_garbageToken_is200() throws Exception {
        mockMvc.perform(get(MET_URL).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk());
    }

    @Test
    void met_expiredToken_is200() throws Exception {
        mockMvc.perform(get(MET_URL).header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isOk());
    }

    // A blank idToken is rejected by validation (400). Getting 400 and not 401
    // proves the request reached the controller: the bad token wasn't read.
    @Test
    void authGoogle_garbageToken_reachesTheController() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
