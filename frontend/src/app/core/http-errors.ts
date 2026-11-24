import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from './models';

const GENERIC_MESSAGE = 'Something went wrong. Please try again.';

function toHttpError(error: unknown): HttpErrorResponse | null {
  if (error instanceof HttpErrorResponse) {
    return error;
  }
  if (error instanceof Error && error.cause instanceof HttpErrorResponse) {
    return error.cause;
  }
  return null;
}

export function httpStatus(error: unknown): number | null {
  return toHttpError(error)?.status ?? null;
}

export function problemMessage(error: unknown): string {
  const response = toHttpError(error);
  if (!response) {
    return GENERIC_MESSAGE;
  }
  if (response.status === 0) {
    return "Can't reach the server. Check your connection and try again.";
  }

  const problem = response.error as ProblemDetail | null;
  const firstFieldError = problem?.errors ? Object.values(problem.errors)[0] : undefined;
  return firstFieldError ?? problem?.detail ?? GENERIC_MESSAGE;
}