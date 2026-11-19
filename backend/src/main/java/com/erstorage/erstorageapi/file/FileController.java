package com.erstorage.erstorageapi.file;

import com.erstorage.erstorageapi.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam("file")MultipartFile file,
                               @RequestParam(value = "folderId", required = false) UUID folderId) {
        return fileService.upload(CurrentUser.id(jwt), folderId, file);
    }

    @GetMapping("/{id}/download")
    public DownloadUrlResponse download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return fileService.getDownloadUrl(CurrentUser.id(jwt), id);
    }

    @PatchMapping("/{id}/rename")
    public FileResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody RenameFileRequest request) {
        return fileService.rename(CurrentUser.id(jwt), id, request);
    }

    @PatchMapping("/{id}/move")
    public FileResponse move(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                             @RequestBody MoveFileRequest request) {
        return fileService.move(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        fileService.delete(CurrentUser.id(jwt), id);
    }
}
