package com.erstorage.erstorageapi;

import com.erstorage.erstorageapi.auth.LoginRequest;
import com.erstorage.erstorageapi.auth.LoginResponse;
import com.erstorage.erstorageapi.auth.RegisterRequest;
import com.erstorage.erstorageapi.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "erstorage.storage.create-bucket-on-startup=false"
)
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "password123";

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    protected StorageService storageService;

    protected RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status ->true, (request, response) -> { })
                .build();
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID()+"@example.com";
    }

    protected String registerAndLogin(String email) {
        client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, PASSWORD, "Test User"))
                .retrieve()
                .toBodilessEntity();

        LoginResponse login = client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, PASSWORD))
                .retrieve()
                .body(LoginResponse.class);

        return login.accessToken();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
