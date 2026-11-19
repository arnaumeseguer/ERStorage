package com.erstorage.erstorageapi.folder;

import com.erstorage.erstorageapi.file.FileResponse;

import java.util.List;

public record FolderContentsResponse(
        FolderResponse folder,
        List<BreadcrumbItem> breadcrumbs,
        List<FolderResponse> folders,
        List<FileResponse> files
) {
}