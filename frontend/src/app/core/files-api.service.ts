import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  BrowserItem,
  DownloadUrlResponse,
  FileResponse,
  FolderContentsResponse,
  FolderResponse,
} from './models';

@Injectable({ providedIn: 'root' })
export class FilesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  getContents(folderId: string | null): Observable<FolderContentsResponse> {
    const url = folderId
      ? `${this.baseUrl}/folders/${folderId}`
      : `${this.baseUrl}/folders/root`;
    return this.http.get<FolderContentsResponse>(url);
  }

  createFolder(name: string, parentId: string | null): Observable<FolderResponse> {
    return this.http.post<FolderResponse>(`${this.baseUrl}/folders`, { name, parentId });
  }

  rename(target: BrowserItem, name: string): Observable<unknown> {
    return this.http.patch(`${this.urlOf(target)}/rename`, { name });
  }

  move(target: BrowserItem, destinationId: string | null): Observable<unknown> {
    const body =
      target.kind === 'folder' ? { parentId: destinationId } : { folderId: destinationId };
    return this.http.patch(`${this.urlOf(target)}/move`, body);
  }

  remove(target: BrowserItem): Observable<unknown> {
    return this.http.delete(this.urlOf(target));
  }

  getDownloadUrl(fileId: string): Observable<DownloadUrlResponse> {
    return this.http.get<DownloadUrlResponse>(`${this.baseUrl}/files/${fileId}/download`);
  }

  upload(file: File, folderId: string | null): Observable<HttpEvent<FileResponse>> {
    const form = new FormData();
    form.append('file', file);
    if (folderId) {
      form.append('folderId', folderId);
    }
    return this.http.post<FileResponse>(`${this.baseUrl}/files`, form, {
      reportProgress: true,
      observe: 'events',
    });
  }

  private urlOf(target: BrowserItem): string {
    const collection = target.kind === 'folder' ? 'folders' : 'files';
    return `${this.baseUrl}/${collection}/${target.item.id}`;
  }
}