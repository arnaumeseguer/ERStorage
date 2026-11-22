import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize, retry, timeout, timer } from 'rxjs';
import { environment } from '../../environments/environment';

export type ServerStatus = 'checking' | 'online' | 'offline';

const ATTEMPT_TIMEOUT_MS = 20_000;
const MAX_RETRIES = 12;
const RETRY_DELAY_MS = 5_000;
const SLOW_AFTER_MS = 3_000;

@Injectable({ providedIn: 'root' })
export class ServerStatusService {
  private readonly http = inject(HttpClient);

  readonly status = signal<ServerStatus>('checking');
  readonly isSlow = signal(false);

  private inFlight = false;

  check(): void {
    if (this.inFlight) {
      return;
    }
    this.inFlight = true;
    this.status.set('checking');
    this.isSlow.set(false);

    const slowTimer = setTimeout(() => this.isSlow.set(true), SLOW_AFTER_MS);

    this.http
      .get(`${environment.apiUrl}/actuator/health`)
      .pipe(
        timeout(ATTEMPT_TIMEOUT_MS),
        retry({ count: MAX_RETRIES, delay: () => timer(RETRY_DELAY_MS) }),
        finalize(() => {
          clearTimeout(slowTimer);
          this.inFlight = false;
        }),
      )
      .subscribe({
        next: () => this.status.set('online'),
        error: () => this.status.set('offline'),
      });
  }
}