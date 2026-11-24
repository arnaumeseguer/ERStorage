import { Component, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { UploadDialog } from '../upload-dialog/upload-dialog';
import {
  LucideArrowDown,
  LucideArrowUp,
  LucideDownload,
  LucideDynamicIcon,
  LucideFolder,
  LucideFolderInput,
  LucideFolderOpen,
  LucideFolderPlus,
  LucidePencil,
  LucideSearchX,
  LucideTrash2,
  LucideTriangleAlert,
  LucideX,
  LucideUpload,
} from '@lucide/angular';
import { AuthService } from '../../../core/auth.service';
import { breadcrumbTrail } from '../../../core/breadcrumbs';
import { FilesApiService } from '../../../core/files-api.service';
import { httpStatus, problemMessage } from '../../../core/http-errors';
import { BrowserItem, FileResponse } from '../../../core/models';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';
import { fileIcon } from '../../../shared/file-icon';
import { FileSizePipe } from '../../../shared/file-size-pipe';
import { NameDialog } from '../../../shared/name-dialog/name-dialog';
import { SortKey, sortItems } from '../../../shared/sorting';
import { MoveDialog } from '../move-dialog/move-dialog';
import { Sidebar } from '../sidebar/sidebar';

type DialogState =
  | { type: 'create-folder' }
  | { type: 'upload' }
  | { type: 'rename' | 'move' | 'delete'; target: BrowserItem };

function targetOf(state: DialogState | null, type: 'rename' | 'move' | 'delete'): BrowserItem | null {
  if (!state || state.type !== type || !('target' in state)) {
    return null;
  }
  return state.target;
}

@Component({
  selector: 'app-browser-page',
  imports: [
    RouterLink,
    DatePipe,
    FileSizePipe,
    Sidebar,
    NameDialog,
    ConfirmDialog,
    MoveDialog,
    LucideDynamicIcon,
    LucideDownload,
    LucideFolder,
    LucideFolderInput,
    LucideFolderOpen,
    LucideFolderPlus,
    LucidePencil,
    LucideSearchX,
    LucideTrash2,
    LucideTriangleAlert,
    LucideX,
    LucideUpload,
    UploadDialog,
  ],
  templateUrl: './browser-page.html',
  styleUrl: './browser-page.scss',
})
export class BrowserPage {
  private readonly api = inject(FilesApiService);
  private readonly auth = inject(AuthService);

  readonly folderId = input<string>();

  protected readonly contents = rxResource({
    params: () => ({ folderId: this.folderId() ?? null }),
    stream: ({ params }) => this.api.getContents(params.folderId),
  });

  protected readonly sortKey = signal<SortKey>('name');
  protected readonly sortAscending = signal(true);
  protected readonly sortIcon = computed(() =>
    this.sortAscending() ? LucideArrowUp : LucideArrowDown,
  );

  protected readonly fileIcon = fileIcon;

  protected readonly trail = computed(() =>
    this.contents.hasValue() ? breadcrumbTrail(this.contents.value()) : [],
  );

  protected readonly title = computed(() => this.trail().at(-1)?.name ?? 'My files');

  protected readonly folders = computed(() => {
    const list = this.contents.hasValue() ? this.contents.value().folders : [];
    const key = this.sortKey() === 'size' ? 'name' : this.sortKey();
    return sortItems(list, key, this.sortAscending());
  });

  protected readonly files = computed(() => {
    const list = this.contents.hasValue() ? this.contents.value().files : [];
    return sortItems(list, this.sortKey(), this.sortAscending());
  });

  protected readonly isEmpty = computed(
    () => this.folders().length === 0 && this.files().length === 0,
  );

  protected readonly isNotFound = computed(() => httpStatus(this.contents.error()) === 404);

  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly busy = signal(false);
  protected readonly dialogError = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly downloadingId = signal<string | null>(null);

  protected readonly isCreatingFolder = computed(() => this.dialog()?.type === 'create-folder');
  protected readonly isUploadOpen = computed(() => this.dialog()?.type === 'upload');
  protected readonly renameTarget = computed(() => targetOf(this.dialog(), 'rename'));
  protected readonly moveTarget = computed(() => targetOf(this.dialog(), 'move'));
  protected readonly deleteTarget = computed(() => targetOf(this.dialog(), 'delete'));

  protected sortBy(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortAscending.update((ascending) => !ascending);
    } else {
      this.sortKey.set(key);
      this.sortAscending.set(true);
    }
  }

  protected openDialog(state: DialogState): void {
    this.dialogError.set(null);
    this.dialog.set(state);
  }

  protected closeDialog(): void {
    if (this.busy()) {
      return;
    }
    this.dialog.set(null);
    this.dialogError.set(null);
  }

  protected createFolder(name: string): void {
    this.run(this.api.createFolder(name, this.folderId() ?? null));
  }

  protected rename(target: BrowserItem, name: string): void {
    this.run(this.api.rename(target, name));
  }

  protected move(target: BrowserItem, destinationId: string | null): void {
    this.run(this.api.move(target, destinationId));
  }

  protected remove(target: BrowserItem): void {
    this.run(this.api.remove(target), true);
  }

  protected deleteMessage(target: BrowserItem): string {
    return target.kind === 'folder'
      ? `“${target.item.name}” and everything inside it will be deleted. This can't be undone.`
      : `“${target.item.name}” will be deleted. This can't be undone.`;
  }

  protected download(file: FileResponse): void {
    if (this.downloadingId()) {
      return;
    }
    this.downloadingId.set(file.id);
    this.actionError.set(null);

    this.api.getDownloadUrl(file.id).subscribe({
      next: ({ url }) => {
        this.downloadingId.set(null);
        window.location.assign(url);
      },
      error: (error) => {
        this.downloadingId.set(null);
        this.actionError.set(problemMessage(error));
      },
    });
  }

    protected onUploaded(): void {
    this.contents.reload();
    this.auth.loadCurrentUser().subscribe();
  }

  private run(request: Observable<unknown>, refreshUser = false): void {
    this.busy.set(true);
    this.dialogError.set(null);

    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.dialog.set(null);
        this.contents.reload();
        if (refreshUser) {
          this.auth.loadCurrentUser().subscribe();
        }
      },
      error: (error) => {
        this.busy.set(false);
        this.dialogError.set(problemMessage(error));
      },
    });
  }
}