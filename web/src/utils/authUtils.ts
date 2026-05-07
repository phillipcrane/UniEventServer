import type { HttpError } from '../types';

export function createHttpError(status: number, message: string): HttpError {
  return Object.assign(new Error(message), { status });
}

// isValidEmail uses a simple check: something@something.something.
// A full RFC-5322 regex would be hundreds of characters long and still not perfect;
// this is intentionally lightweight because the backend re-validates anyway.
export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function mapAuthError(error: unknown): string {
  if (error && typeof error === 'object') {
    const e = error as { status?: number; message?: string };
    if (e.status === 401 || e.status === 403) {
      // "Invalid credentials." is Spring Security's generic login failure - rephrase it.
      // Any other 401 message (e.g. "Organizer key expired.") is meaningful and shown as-is.
      if (!e.message || e.message === 'Invalid credentials.') return 'Invalid email or password.';
      return e.message;
    }
    if (e.status === 409 || (e.status !== undefined && e.message && e.message.toLowerCase().includes('already'))) {
      return e.message ?? 'Account already exists.';
    }
    if (e.status === 400) {
      return e.message ?? 'Invalid input. Please check your details.';
    }
    if (e.status !== undefined && e.message) {
      return e.message;
    }
  }
  return 'Something went wrong. Please try again.';
}
