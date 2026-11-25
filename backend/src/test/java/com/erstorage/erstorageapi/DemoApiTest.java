package com.erstorage.erstorageapi;

import com.erstorage.erstorageapi.demo.DemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DemoApiTest extends IntegrationTestBase {

    @LocalServerPort
    private int localPort;

    @Autowired
    private DemoService demoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + localPort)
                .defaultStatusHandler(status -> true, (request, response) -> { })
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> startDemo() {
        ResponseEntity<Map> response = client().post()
                .uri("/api/v1/auth/demo")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (Map<String, Object>) response.getBody();
    }

    @Test
    @SuppressWarnings("unchecked")
    void startingADemoCreatesAGuestWithSampleFolders() {
        Map<String, Object> body = startDemo();
        Map<String, Object> user = (Map<String, Object>) body.get("user");

        assertThat(user.get("guest")).isEqualTo(true);
        assertThat(user.get("expiresAt")).isNotNull();
        assertThat(((Number) body.get("expiresInSeconds")).longValue()).isBetween(3500L, 3600L);

        ResponseEntity<Map> root = client().get()
                .uri("/api/v1/folders/root")
                .header("Authorization", "Bearer " + body.get("accessToken"))
                .retrieve()
                .toEntity(Map.class);

        List<Map<String, Object>> folders = (List<Map<String, Object>>) root.getBody().get("folders");
        assertThat(folders)
                .extracting(folder -> folder.get("name"))
                .containsExactlyInAnyOrder("Documents", "Photos", "Projects");
    }

    @Test
    @SuppressWarnings("unchecked")
    void expiredGuestsAreDeleted() {
        Map<String, Object> user = (Map<String, Object>) startDemo().get("user");
        UUID guestId = UUID.fromString((String) user.get("id"));

        jdbcTemplate.update("update users set expires_at = now() - interval '1 minute' where id = ?", guestId);
        demoService.deleteExpiredGuests();

        Integer remainingUsers = jdbcTemplate.queryForObject(
                "select count(*) from users where id = ?", Integer.class, guestId);
        Integer remainingFolders = jdbcTemplate.queryForObject(
                "select count(*) from folders where owner_id = ?", Integer.class, guestId);

        assertThat(remainingUsers).isZero();
        assertThat(remainingFolders).isZero();
    }
}