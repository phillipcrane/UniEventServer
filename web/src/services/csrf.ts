import { API_AUTH_CSRF_TOKEN, BACKEND_URL, CSRF_COOKIE_NAME } from '../constants';
import { createHttpError } from '../utils/authUtils';

let csrfToken: string | null = null;

export function getCsrfToken(): string {
  if (csrfToken !== null) {
    return csrfToken;
  }
  return readCookie(CSRF_COOKIE_NAME);
}

export function setCsrfToken(token: string | null): void {
  csrfToken = token ?? '';
}

export async function fetchCsrfToken(): Promise<string> {
  const response = await fetch(`${BACKEND_URL}${API_AUTH_CSRF_TOKEN}`, {
    method: 'GET',
    credentials: 'include',
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as Record<string, unknown>;
    throw createHttpError(
      response.status,
      (body['message'] as string | undefined) ?? response.statusText,
    );
  }

  const data = await response.json() as { csrfToken?: string };
  const token = typeof data.csrfToken === 'string' ? data.csrfToken : '';
  setCsrfToken(token);
  return token;
}

export async function ensureCsrfToken(): Promise<string> {
  const existing = getCsrfToken();
  if (existing) {
    return existing;
  }

  return fetchCsrfToken();
}

export function resetCsrfTokenForTesting(): void {
  csrfToken = null;
}

function readCookie(name: string): string {
  if (typeof document === 'undefined' || !document.cookie) {
    return '';
  }

  const prefix = `${name}=`;
  const cookie = document.cookie
    .split('; ')
    .find((entry) => entry.startsWith(prefix));

  if (!cookie) {
    return '';
  }

  return decodeURIComponent(cookie.slice(prefix.length));
}
