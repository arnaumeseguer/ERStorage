package com.erstorage.erstorageapi.folder;

import com.erstorage.erstorageapi.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping("/root")
    public FolderContentsResponse root(@AuthenticationPrincipal Jwt jwt) {
        return folderService.getFolderContents(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public FolderContentsResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return folderService.getContents(CurrentUser.id(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody CreateFolderRequest request) {
        return folderService.create(CurrentUser.id(jwt), request);
    }

    @PatchMapping("/{id}/rename")
    public FolderResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody RenameFolderRequest request) {
        return folderService.rename(CurrentUser.id(jwt), id, request);
    }

    @PatchMapping("/{id}/move")
    public FolderResponse move(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @RequestBody MoveFolderRequest request) {
        return folderService.move(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        folderService.delete(CurrentUser.id(jwt), id);
    }
}