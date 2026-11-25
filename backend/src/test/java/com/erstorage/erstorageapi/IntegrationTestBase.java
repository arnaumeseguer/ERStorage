package com.erstorage.erstorageapi;

import com.erstorage.erstorageapi.auth.LoginRequest;
import com.erstorage.erstorageapi.auth.LoginResponse;
import com.erstorage.erstorageapi.auth.RegisterRequest;
import com.erstorage.erstorageapi.folder.CreateFolderRequest;
import com.erstorage.erstorageapi.folder.FolderResponse;
import com.erstorage.erstorageapi.storage.StorageService;
import com.erstorage.erstorageapi.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "erstorage.storage.create-bucket-on-startup=false",
                "erstorage.jwt.secret=test-only-secret-that-is-at-least-32-characters-long",
                "erstorage.limits.registration-enabled=true",
                "erstorage.demo.enabled=true"
        }
)
@Import(TestcontainersConfiguration.class)
abstract class IntegrationTestBase {

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
                .defaultStatusHandler(status -> true, (request, response) -> { })
                .build();
    }

    // ---------- users ----------

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
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

    protected long usedBytes(String token) {
        return client.get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(UserResponse.class)
                .storageUsedBytes();
    }

    // ---------- folders ----------

    protected ResponseEntity<FolderResponse> createFolder(String token, String name, UUID parentId) {
        return client.post()
                .uri("/api/v1/folders")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateFolderRequest(name, parentId))
                .retrieve()
                .toEntity(FolderResponse.class);
    }

    // ---------- files ----------

    protected <T> ResponseEntity<T> upload(String token, String fileName, String content, Class<T> responseType) {
        return upload(token, fileName, content, null, responseType);
    }

    protected <T> ResponseEntity<T> upload(String token, String fileName, String content, UUID folderId,
                                           Class<T> responseType) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        if (folderId != null) {
            form.add("folderId", folderId.toString());
        }

        return client.post()
                .uri("/api/v1/files")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .toEntity(responseType);
    }
}