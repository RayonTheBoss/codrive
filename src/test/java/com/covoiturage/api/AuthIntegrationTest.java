package com.covoiturage.api;

import com.covoiturage.enums.StatutCompte;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Passager;
import com.covoiturage.model.User;
import com.covoiturage.repository.UserRepository;
import com.covoiturage.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:codrive-auth-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "codrive.demo-data=false"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthService authService;

    @Test
    void registersHashedCredentialsAndRejectsSpoofedIdentityAndLoggedOutTokens() throws Exception {
        String password = " caf\u00e9 test 123 ";
        Map<String, String> registration = Map.of("nom", "Test", "prenom", "Amina",
                "email", "AMINA@example.test", "telephone", "22000000", "motDePasse", password,
                "role", "PASSAGER");
        JsonNode session = responseJson(mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.role").value("PASSAGER"))
                .andExpect(jsonPath("$.email").value("amina@example.test"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist()));
        assertThat(Instant.parse(session.get("expiresAt").asText())).isAfter(Instant.now());
        assertThat(userRepository.findById(session.get("id").asInt()).orElseThrow().getMotDePasse())
                .startsWith("{pbkdf2-sha256}600000$")
                .doesNotContain(password);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/users").header("X-User-Email", "admin@mail.com")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));

        JsonNode login = responseJson(login("AMINA@EXAMPLE.TEST", password).andExpect(status().isOk()));
        String bearer = bearer(login);
        assertThat(login.get("token").asText()).isNotEqualTo(session.get("token").asText());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PASSAGER"));
        mvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(session)))
                .andExpect(status().isOk());
    }

    @Test
    void upgradesLegacyPasswordAndInvalidatesSessionsAcrossLockoutAndReactivation() throws Exception {
        String email = "legacy@example.test";
        String legacyPassword = "abcd";
        User legacy = userRepository.save(new Passager("Legacy", "User", email, legacyPassword, "22000001"));
        authService.creerCompte(new Admin("Admin", "Test", "admin@example.test", "test-admin-password", "22000002"));
        String admin = bearer(responseJson(login("admin@example.test", "test-admin-password").andExpect(status().isOk())));
        String initial = bearer(responseJson(login(email, legacyPassword).andExpect(status().isOk())));
        assertThat(userRepository.findById(legacy.getId()).orElseThrow().getMotDePasse())
                .startsWith("{pbkdf2-sha256}600000$");

        for (int i = 0; i < 3; i++) {
            login(email, "incorrect").andExpect(status().isUnauthorized());
        }
        User blocked = userRepository.findById(legacy.getId()).orElseThrow();
        assertThat(blocked.getTentativesConnexion()).isEqualTo(3);
        assertThat(blocked.getStatutCompte()).isEqualTo(StatutCompte.BLOQUE);
        login(email, legacyPassword).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/admin/users/{id}/reactivate", legacy.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(legacy.getId()).orElseThrow().getTentativesConnexion()).isZero();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, initial))
                .andExpect(status().isUnauthorized());

        String fresh = bearer(responseJson(login(email, legacyPassword).andExpect(status().isOk())));
        mvc.perform(post("/api/admin/users/{id}/suspend", legacy.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, fresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsFileAndLocalOriginsWithBearerHeader() throws Exception {
        for (String origin : new String[] {"null", "http://127.0.0.1:5500", "http://localhost:8000"}) {
            mvc.perform(options("/api/auth/me").header(HttpHeaders.ORIGIN, origin)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization"));
        }
        mvc.perform(options("/api/auth/me").header(HttpHeaders.ORIGIN, "https://external.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isForbidden());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "motDePasse", password))));
    }

    private JsonNode responseJson(ResultActions response) throws Exception {
        return objectMapper.readTree(response.andReturn().getResponse().getContentAsByteArray());
    }

    private String bearer(JsonNode session) {
        return "Bearer " + session.get("token").asText();
    }
}
