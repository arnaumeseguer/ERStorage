import { Pipe, PipeTransform } from '@angular/core';

const UNITS = ['KB', 'MB', 'GB', 'TB'];

@Pipe({ name: 'fileSize' })
export class FileSizePipe implements PipeTransform {
  transform(bytes: number | null | undefined): string {
    if (bytes == null) {
      return '—';
    }
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    let value = bytes / 1024;
    let unit = 0;
    while (value >= 1024 && unit < UNITS.length - 1) {
      value /= 1024;
      unit++;
    }
    return `${value.toFixed(value < 10 ? 1 : 0)} ${UNITS[unit]}`;
  }
}
