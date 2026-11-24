import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { Subscription } from 'rxjs';
import {
  LucideCircleCheck,
  LucideDynamicIcon,
  LucideRotateCw,
  LucideUpload,
  LucideX,
} from '@lucide/angular';
import { FilesApiService } from '../../../core/files-api.service';
import { problemMessage } from '../../../core/http-errors';
import { FileResponse } from '../../../core/models';
import { fileIcon } from '../../../shared/file-icon';
import { FileSizePipe } from '../../../shared/file-size-pipe';

type UploadStatus = 'pending' | 'uploading' | 'processing' | 'done' | 'error' | 'cancelled';

interface UploadItem {
  id: number;
  file: File;
  status: UploadStatus;
  progress: number;
  error: string | null;
  canRetry: boolean;
}

const MAX_FILE_BYTES = 100 * 1024 * 1024;
const BLOCKED_EXTENSIONS = new Set(['exe', 'bat', 'cmd', 'msi', 'com', 'scr', 'ps1', 'vbs', 'sh']);

function validateFile(file: File): string | null {
  if (file.size > MAX_FILE_BYTES) {
    return 'Larger than 100 MB.';
  }
  const dot = file.name.lastIndexOf('.');
  const extension = dot > 0 ? file.name.slice(dot + 1).toLowerCase() : '';
  if (BLOCKED_EXTENSIONS.has(extension)) {
    return 'This file type is not allowed.';
  }
  return null;
}

@Component({
  selector: 'app-upload-dialog',
  imports: [
    FileSizePipe,
    LucideDynamicIcon,
    LucideUpload,
    LucideX,
    LucideRotateCw,
    LucideCircleCheck,
  ],
  templateUrl: './upload-dialog.html',
  styleUrl: './upload-dialog.scss',
})
export class UploadDialog {
  private readonly api = inject(FilesApiService);

  readonly folderId = input<string | null>(null);
  readonly folderName = input('My files');

  readonly uploaded = output<FileResponse>();
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  protected readonly items = signal<UploadItem[]>([]);
  protected readonly dragging = signal(false);
  protected readonly fileIcon = fileIcon;

  protected readonly isUploading = computed(() =>
    this.items().some((item) => item.status === 'pending' || item.status === 'uploading' || item.status === 'processing'),
  );

  protected readonly summary = computed(() => {
    const items = this.items();
    const done = items.filter((item) => item.status === 'done').length;
    const failed = items.filter((item) => item.status === 'error').length;
    return failed > 0
      ? `${done} of ${items.length} uploaded · ${failed} failed`
      : `${done} of ${items.length} uploaded`;
  });

  private nextId = 0;
  private current: Subscription | null = null;

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
    inject(DestroyRef).onDestroy(() => this.current?.unsubscribe());
  }

  protected chooseFiles(): void {
    this.fileInput().nativeElement.click();
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(input.files);
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDragLeave(): void {
    this.dragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.addFiles(event.dataTransfer?.files ?? null);
  }

  protected cancel(item: UploadItem): void {
    if (item.status === 'uploading') {
      this.current?.unsubscribe();
      this.current = null;
    }
    this.patch(item.id, { status: 'cancelled' });
    this.startNext();
  }

  protected retry(item: UploadItem): void {
    this.patch(item.id, { status: 'pending', progress: 0, error: null });
    this.startNext();
  }

  protected close(): void {
    this.current?.unsubscribe();
    this.current = null;
    this.items.update((items) =>
      items.map((item) =>
        item.status === 'pending' || item.status === 'uploading'
          ? { ...item, status: 'cancelled' }
          : item,
      ),
    );
    this.dialog().nativeElement.close();
  }

  protected onEscape(event: Event): void {
    event.preventDefault();
    this.close();
  }

  private addFiles(fileList: FileList | null): void {
    if (!fileList || fileList.length === 0) {
      return;
    }
    const newItems = Array.from(fileList, (file): UploadItem => {
      const error = validateFile(file);
      return {
        id: this.nextId++,
        file,
        status: error ? 'error' : 'pending',
        progress: 0,
        error,
        canRetry: false,
      };
    });
    this.items.update((items) => [...items, ...newItems]);
    this.startNext();
  }

  private startNext(): void {
    if (this.current) {
      return;
    }
    const item = this.items().find((candidate) => candidate.status === 'pending');
    if (!item) {
      return;
    }

    this.patch(item.id, { status: 'uploading', progress: 0 });

    this.current = this.api.upload(item.file, this.folderId()).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const progress = event.total ? Math.round((event.loaded / event.total) * 100) : 0;
          this.patch(item.id, progress < 100 ? { progress } : { progress: 100, status: 'processing' });
        } else if (event.type === HttpEventType.Response) {
          this.patch(item.id, { status: 'done', progress: 100 });
          if (event.body) {
            this.uploaded.emit(event.body);
          }
        }
      },
      error: (error) => {
        this.patch(item.id, { status: 'error', error: problemMessage(error), canRetry: true });
        this.finishCurrent();
      },
      complete: () => this.finishCurrent(),
    });
  }

  private finishCurrent(): void {
    this.current = null;
    this.startNext();
  }

  private patch(id: number, changes: Partial<UploadItem>): void {
    this.items.update((items) =>
      items.map((item) => (item.id === id ? { ...item, ...changes } : item)),
    );
  }
}