import { Component, ElementRef, afterNextRender, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.html',
})
export class ConfirmDialog {
  readonly heading = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Confirm');
  readonly busy = input(false);
  readonly error = input<string | null>(null);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
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