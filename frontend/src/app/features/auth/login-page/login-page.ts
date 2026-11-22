import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { ProblemDetail } from '../../../core/models';
import { ServerStatusBanner } from '../../../shared/server-status-banner/server-status-banner';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, ServerStatusBanner],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.fieldErrors.set({});

    const { email, password } = this.form.getRawValue();

    this.auth.login(email.trim(), password).subscribe({
      next: () => this.router.navigateByUrl('/files'),
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.showError(error);
      },
    });
  }

  protected hasError(field: 'email' | 'password', error: string): boolean {
    const control = this.form.controls[field];
    return control.touched && control.hasError(error);
  }

  private showError(error: HttpErrorResponse): void {
    if (error.status === 0) {
      this.errorMessage.set("Can't reach the server. Check your connection and try again.");
      return;
    }
    if (error.status === 401) {
      this.errorMessage.set('Wrong email or password.');
      return;
    }

    const problem = error.error as ProblemDetail | null;
    if (problem?.errors) {
      this.fieldErrors.set(problem.errors);
      return;
    }
    this.errorMessage.set(problem?.detail ?? 'Something went wrong. Please try again.');
  }
}