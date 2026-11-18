package com.erstorage.erstorageapi.folder;

import java.util.List;

public record FolderContentsResponse(
        FolderResponse folder,
        List<BreadcrumbItem> breadcrumbs,
        List<FolderResponse> folders
) {
}