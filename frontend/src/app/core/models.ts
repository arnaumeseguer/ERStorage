export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  storageUsedBytes: number;
  storageQuotaBytes: number;
}

export interface LoginResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: UserResponse;
}

export interface FolderResponse {
  id: string;
  name: string;
  parentId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BreadcrumbItem {
  id: string;
  name: string;
}

export interface FileResponse {
  id: string;
  name: string;
  folderId: string | null;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
  updatedAt: string;
}

export interface FolderContentsResponse {
  folder: FolderResponse | null;
  breadcrumbs: BreadcrumbItem[];
  folders: FolderResponse[];
  files: FileResponse[];
}

export interface DownloadUrlResponse {
  url: string;
  expiresInSeconds: number;
}

export interface ProblemDetail {
  title?: string;
  status: number;
  detail?: string;
  errors?: Record<string, string>;
}