package com.erstorage.erstorageapi;

import com.erstorage.erstorageapi.auth.LoginRequest;
import com.erstorage.erstorageapi.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiTest extends IntegrationTestBase {

    @Test
    void registerCreatesTheUserWithoutExposingThePassword() {
        String email = uniqueEmail();

        ResponseEntity<String> response = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, PASSWORD, "Arnau"))
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .contains(email)
                .doesNotContain("password");
    }

    @Test
    void registeringTheSameEmailTwiceReturnsConflict() {
        String email = uniqueEmail();
        registerAndLogin(email);

        ResponseEntity<Void> response = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, PASSWORD, "Again"))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void invalidRegistrationReturnsOneErrorPerField() {
        ResponseEntity<Map<String, Object>> response = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("not-an-email", "123", ""))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() { });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, Object> errors = (Map<String, Object>) response.getBody().get("errors");

        assertThat(errors).containsKeys("email", "password", "displayName");
    }

    @Test
    void loginWithAWrongPasswordIsRejected() {
        String email = uniqueEmail();
        registerAndLogin(email);

        ResponseEntity<Void> response = client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "wrong-password"))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meRequiresAValidToken() {
        String email = uniqueEmail();
        String token = registerAndLogin(email);

        ResponseEntity<Void> withoutToken = client.get()
                .uri("/api/v1/users/me")
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<String> withToken = client.get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .toEntity(String.class);

        assertThat(withoutToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(withToken.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(withToken.getBody()).contains(email);
    }
}