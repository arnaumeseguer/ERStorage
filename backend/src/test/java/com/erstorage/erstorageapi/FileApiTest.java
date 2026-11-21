package com.erstorage.erstorageapi;

import com.erstorage.erstorageapi.file.FileResponse;
import com.erstorage.erstorageapi.user.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FileApiTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private <T> ResponseEntity<T> upload(String token, String fileName, String content, Class<T> responseType) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        return client.post()
                .uri("/api/v1/files")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .toEntity(responseType);
    }

    private long usedBytes(String token) {
        return client.get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(UserResponse.class)
                .storageUsedBytes();
    }

    @Test
    void uploadStoresTheBytesAndCountsThemInTheQuota() {
        String token = registerAndLogin(uniqueEmail());

        ResponseEntity<FileResponse> response = upload(token, "hello.txt", "hello", FileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("hello.txt");
        assertThat(usedBytes(token)).isEqualTo(5);
        verify(storageService).store(startsWith("users/"), any(), eq(5L), anyString());
    }

    @Test
    void executableFilesAreRejected() {
        String token = registerAndLogin(uniqueEmail());

        ResponseEntity<String> response = upload(token, "virus.exe", "MZ", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void uploadsThatDoNotFitInTheQuotaAreRejected() {
        String email = uniqueEmail();
        String token = registerAndLogin(email);
        jdbcTemplate.update("UPDATE users SET storage_quota_bytes = 3 WHERE email = ?", email);

        ResponseEntity<String> response = upload(token, "hello.txt", "hello", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
        assertThat(usedBytes(token)).isZero();
        verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void deletingAFileGivesTheSpaceBackAndRemovesTheBytes() {
        String token = registerAndLogin(uniqueEmail());
        FileResponse file = upload(token, "temp.txt", "temporary", FileResponse.class).getBody();

        ResponseEntity<Void> response = client.delete()
                .uri("/api/v1/files/{id}", file.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usedBytes(token)).isZero();
        verify(storageService).delete(startsWith("users/"));
    }
}