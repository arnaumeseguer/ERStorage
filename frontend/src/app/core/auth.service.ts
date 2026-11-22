import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse, UserResponse } from './models';

const TOKEN_KEY = 'erstorage.token';
const EXPIRES_AT_KEY = 'erstorage.tokenExpiresAt';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly token = signal<string | null>(this.readStoredToken());

  readonly currentUser = signal<UserResponse | null>(null);
  readonly isLoggedIn = computed(() => this.token() !== null);

  getToken(): string | null {
    return this.token();
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/api/v1/auth/login`, { email, password })
      .pipe(tap((response) => this.storeSession(response)));
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${environment.apiUrl}/api/v1/users/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): void {
    this.clearStorage();
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  private storeSession(response: LoginResponse): void {
    const expiresAt = Date.now() + response.expiresInSeconds * 1000;
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));
    this.token.set(response.accessToken);
    this.currentUser.set(response.user);
  }

  private readStoredToken(): string | null {
    const token = localStorage.getItem(TOKEN_KEY);
    const expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY));

    if (!token || !expiresAt || Date.now() >= expiresAt) {
      this.clearStorage();
      return null;
    }
    return token;
  }

  private clearStorage(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
  }
}