package com.kg.museumly.backend;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kg.museumly.backend.security.google.GoogleTokenVerificationClient;
import com.kg.museumly.backend.security.jwt.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests: auth edge cases through the full HTTP stack + DB (user rows, bad/expired/forged JWTs, validation).
// Extend: add a @Test that crafts a token (see tokenWith) or a login (see stubGoogle) and asserts status + $.code.
// Not @Transactional on purpose: MockMvc calls commit, so every test uses unique emails/subs.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthEdgeCasesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private GoogleTokenVerificationClient googleClient;

    @Test
    void login_createsOneUserRow_andRepeatedLoginDoesNotAddAnother() throws Exception {
        String sub = UUID.randomUUID().toString();
        stubGoogle("tok-1", sub, sub + "@example.com");

        login("tok-1");
        login("tok-1");

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE google_sub = ?", Integer.class, sub);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void me_withValidTokenForDeletedUser_returns401UserNotFound() throws Exception {
        String token = jwtService.issueToken(UUID.randomUUID());   // well-formed JWT, but no such user

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void me_withoutAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withExpiredToken_returns401() throws Exception {
        String token = tokenWith(jwtEncoder, Instant.now().minusSeconds(3600));

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withTokenSignedByAnotherKey_returns401() throws Exception {
        byte[] otherKey = "another-secret-another-secret-32b".getBytes();
        JwtEncoder forger = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));

        String token = tokenWith(forger, Instant.now().plusSeconds(3600));

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void googleLogin_withBlankIdToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private String tokenWith(JwtEncoder encoder, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("museumly-backend")
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void stubGoogle(String idToken, String sub, String email) throws Exception {
        GoogleIdToken token = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(sub);
        payload.setEmail(email);
        when(token.getPayload()).thenReturn(payload);
        when(googleClient.verify(idToken)).thenReturn(token);
    }

    private void login(String idToken) throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + idToken + "\"}"))
                .andExpect(status().isOk());
    }
}
