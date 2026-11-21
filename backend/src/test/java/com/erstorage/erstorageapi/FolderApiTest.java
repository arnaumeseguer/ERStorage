package com.erstorage.erstorageapi;

import com.erstorage.erstorageapi.file.FileResponse;
import com.erstorage.erstorageapi.folder.BreadcrumbItem;
import com.erstorage.erstorageapi.folder.CreateFolderRequest;
import com.erstorage.erstorageapi.folder.FolderContentsResponse;
import com.erstorage.erstorageapi.folder.FolderResponse;
import com.erstorage.erstorageapi.folder.MoveFolderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class FolderApiTest extends IntegrationTestBase {

    @Test
    void openingANestedFolderReturnsItsBreadcrumbs() {
        String token = registerAndLogin(uniqueEmail());
        FolderResponse work = createFolder(token, "Work", null).getBody();
        FolderResponse year = createFolder(token, "2026", work.id()).getBody();

        FolderContentsResponse contents = client.get()
                .uri("/api/v1/folders/{id}", year.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(FolderContentsResponse.class);

        assertThat(contents.breadcrumbs())
                .extracting(BreadcrumbItem::name)
                .containsExactly("Work", "2026");
    }

    @Test
    void aFolderCannotBeMovedIntoItsOwnSubfolder() {
        String token = registerAndLogin(uniqueEmail());
        FolderResponse work = createFolder(token, "Work", null).getBody();
        FolderResponse year = createFolder(token, "2026", work.id()).getBody();

        ResponseEntity<Void> response = client.patch()
                .uri("/api/v1/folders/{id}/move", work.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MoveFolderRequest(year.id()))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void twoFoldersWithTheSameNameInTheSamePlaceAreRejected() {
        String token = registerAndLogin(uniqueEmail());
        createFolder(token, "Docs", null);

        ResponseEntity<FolderResponse> duplicate = createFolder(token, "Docs", null);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void usersCannotSeeOtherUsersFolders() {
        String ownerToken = registerAndLogin(uniqueEmail());
        String otherToken = registerAndLogin(uniqueEmail());
        FolderResponse privateFolder = createFolder(ownerToken, "Private", null).getBody();

        ResponseEntity<Void> response = client.get()
                .uri("/api/v1/folders/{id}", privateFolder.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletingAFolderRemovesEverythingInsideAndGivesTheSpaceBack() {
        String token = registerAndLogin(uniqueEmail());
        FolderResponse work = createFolder(token, "Work", null).getBody();
        FolderResponse year = createFolder(token, "2026", work.id()).getBody();
        upload(token, "notes.txt", "abc", work.id(), FileResponse.class);
        upload(token, "invoice.txt", "abcde", year.id(), FileResponse.class);
        assertThat(usedBytes(token)).isEqualTo(8);

        ResponseEntity<Void> response = client.delete()
                .uri("/api/v1/folders/{id}", work.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .toBodilessEntity();

        FolderContentsResponse root = client.get()
                .uri("/api/v1/folders/root")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(FolderContentsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usedBytes(token)).isZero();
        assertThat(root.folders()).isEmpty();
        verify(storageService).deleteAll(argThat(keys -> keys.size() == 2));
    }
}