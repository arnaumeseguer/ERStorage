import {
  Component,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { LucideChevronRight, LucideFolder } from '@lucide/angular';
import { breadcrumbTrail } from '../../../core/breadcrumbs';
import { FilesApiService } from '../../../core/files-api.service';
import { BrowserItem } from '../../../core/models';
import { nameCollator } from '../../../shared/sorting';

@Component({
  selector: 'app-move-dialog',
  imports: [LucideFolder, LucideChevronRight],
  templateUrl: './move-dialog.html',
  styleUrl: './move-dialog.scss',
})
export class MoveDialog {
  private readonly api = inject(FilesApiService);

  readonly target = input.required<BrowserItem>();
  readonly busy = input(false);
  readonly error = input<string | null>(null);

  readonly confirmed = output<string | null>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly currentParentId = computed(() => {
    const target = this.target();
    return target.kind === 'folder' ? target.item.parentId : target.item.folderId;
  });

  protected readonly location = linkedSignal(() => this.currentParentId());

  protected readonly contents = rxResource({
    params: () => ({ folderId: this.location() }),
    stream: ({ params }) => this.api.getContents(params.folderId),
  });

  protected readonly trail = computed(() =>
    this.contents.hasValue() ? breadcrumbTrail(this.contents.value()) : [],
  );

  protected readonly folders = computed(() => {
    if (!this.contents.hasValue()) {
      return [];
    }
    const target = this.target();
    return this.contents
      .value()
      .folders.filter((folder) => !(target.kind === 'folder' && folder.id === target.item.id))
      .sort((a, b) => nameCollator.compare(a.name, b.name));
  });

  protected readonly isCurrentLocation = computed(
    () => this.location() === this.currentParentId(),
  );

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  protected open(folderId: string | null): void {
    this.location.set(folderId);
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  protected onCancel(event: Event): void {
    if (this.busy()) {
      event.preventDefault();
    }
  }

  protected onEscape(event: Event): void {
  event.preventDefault();
  if (!this.busy()) {
    this.close();
  }
}
}