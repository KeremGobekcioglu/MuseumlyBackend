package com.kg.museumly.backend;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.jayway.jsonpath.JsonPath;
import com.kg.museumly.backend.security.GoogleTokenVerificationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleTokenVerificationClient googleClient;

    @Test
    void googleLogin_thenMe_returnsTheSameUser() throws Exception {
        String googleSub = UUID.randomUUID().toString();
        stubGoogleToken("google-token-1", googleSub, "kerem@example.com");

        String accessToken = login("google-token-1");

        mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kerem@example.com"));
    }

    @Test
    void secondLogin_withSameGoogleAccount_returnsSameUser() throws Exception {
        String googleSub = UUID.randomUUID().toString();
        stubGoogleToken("google-token-a", googleSub, "same@example.com");
        stubGoogleToken("google-token-b", googleSub, "same@example.com");

        String firstId = meId(login("google-token-a"));
        String secondId = meId(login("google-token-b"));

        assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    void googleLogin_withInvalidToken_returns401WithCode() throws Exception {
        when(googleClient.verify("bad-token")).thenReturn(null);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"));
    }

    @Test
    void me_withInvalidToken_returns401WithCode() throws Exception {
        mockMvc.perform(get("/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void stubGoogleToken(String idToken, String googleSub, String email) throws Exception {
        GoogleIdToken token = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(googleSub);
        payload.setEmail(email);
        when(token.getPayload()).thenReturn(payload);
        when(googleClient.verify(idToken)).thenReturn(token);
    }

    private String login(String idToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + idToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String meId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}