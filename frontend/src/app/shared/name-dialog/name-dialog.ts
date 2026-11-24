import {
  Component,
  ElementRef,
  afterNextRender,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';

@Component({
  selector: 'app-name-dialog',
  templateUrl: './name-dialog.html',
})
export class NameDialog {
  readonly heading = input.required<string>();
  readonly label = input('Name');
  readonly confirmLabel = input('Save');
  readonly initialValue = input('');
  readonly selectBeforeExtension = input(false);
  readonly busy = input(false);
  readonly error = input<string | null>(null);

  readonly confirmed = output<string>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly nameInput = viewChild.required<ElementRef<HTMLInputElement>>('nameInput');

  protected readonly emptyError = signal(false);

  constructor() {
    afterNextRender(() => {
      const input = this.nameInput().nativeElement;
      input.value = this.initialValue();
      this.dialog().nativeElement.showModal();
      this.selectName(input);
    });
  }

  protected submit(event: Event): void {
    event.preventDefault();
    if (this.busy()) {
      return;
    }

    const name = this.nameInput().nativeElement.value.trim();
    if (!name) {
      this.emptyError.set(true);
      return;
    }
    if (name === this.initialValue()) {
      this.close();
      return;
    }
    this.confirmed.emit(name);
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

  private selectName(input: HTMLInputElement): void {
    const dot = input.value.lastIndexOf('.');
    if (this.selectBeforeExtension() && dot > 0) {
      input.setSelectionRange(0, dot);
    } else {
      input.select();
    }
  }
  
}