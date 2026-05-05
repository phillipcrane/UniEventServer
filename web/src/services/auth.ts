import {
  API_AUTH_LOGIN,
  API_AUTH_LOGOUT,
  API_AUTH_PROFILE,
  API_AUTH_REFRESH,
  API_AUTH_REGISTER,
  API_AUTH_REGISTER_WITH_KEY,
  BACKEND_URL,
  REFRESH_THRESHOLD_MS,
} from '../constants';
import type { AccountRole, AuthApiResponse, SignupRequest, User,
    GenerateOrganizerKeyRequest,
    GenerateOrganizerKeyResponse,
    OrganizerKeyRequestData,
    OrganizerKeyRequestResponse,
    OrganizerKeyVerifyResponse,
    OrganizerRegisterWithKeyRequest,
} from '../types';
import { createHttpError } from '../utils/authUtils';
import { ensureCsrfToken, getCsrfToken, resetCsrfTokenForTesting, setCsrfToken } from './csrf';
import { apiCall } from './http';
import { sanitizeErrorMessage } from '../utils/securityUtils';

type AuthErrorContext = 'login' | 'signup' | 'general';

type SignupInput = SignupRequest & {
  organizerNames?: string[];
};

const USER_KEY = 'unievent_user';

let currentUser: User | null = null;
let tokenExpiresAt: number | null = null;

const listeners: Array<(user: User | null) => void> = [];

export type AuthUser = User;
export type { AccountRole };

function getStorage(): Storage | null {
  if (typeof localStorage === 'undefined') {
    return null;
  }
  if (
    typeof localStorage.getItem !== 'function'
    || typeof localStorage.setItem !== 'function'
    || typeof localStorage.removeItem !== 'function'
  ) {
    return null;
  }
  return localStorage;
}

export { getCsrfToken, setCsrfToken };

export function setCurrentUser(user: User): void {
  currentUser = user;
  getStorage()?.setItem(USER_KEY, JSON.stringify(user));
}

export function clearCurrentUser(): void {
  currentUser = null;
  tokenExpiresAt = null;
  getStorage()?.removeItem(USER_KEY);
}

function clearAuthState(): void {
  setCsrfToken(null);
  clearCurrentUser();
  notifyListeners(null);
}

function redirectToLogin(): void {
  if (typeof window === 'undefined' || window.location.pathname === '/login') {
    return;
  }
  try {
    window.location.assign('/login');
  } catch {
    window.location.href = '/login';
  }
}

export function clearSessionAndRedirect(): void {
  clearAuthState();
  redirectToLogin();
}

export function notifyListeners(user: User | null): void {
  listeners.forEach((callback) => callback(user));
}

export function onUserChanged(callback: (user: User | null) => void): () => void {
  listeners.push(callback);
  callback(getCurrentUser());
  return () => {
    const index = listeners.indexOf(callback);
    if (index !== -1) {
      listeners.splice(index, 1);
    }
  };
}

export function storeTokenExpiry(accessTokenExpiresInMs: number): void {
  tokenExpiresAt = Date.now() + accessTokenExpiresInMs;
}

export function getTokenExpiresAt(): number | null {
  return tokenExpiresAt;
}

export function isTokenExpiredOrExpiringSoon(thresholdMs = REFRESH_THRESHOLD_MS): boolean {
  if (tokenExpiresAt === null) {
    return false;
  }
  return Date.now() >= tokenExpiresAt - thresholdMs;
}

export function normalizeRole(value: unknown): AccountRole | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const normalized = value.trim().toUpperCase();
  if (normalized === 'ORGANIZER' || normalized === 'ROLE_ORGANIZER') {
    return 'organizer';
  }
  if (normalized === 'USER' || normalized === 'ROLE_USER') {
    return 'user';
  }
  if (normalized === 'ADMIN' || normalized === 'ROLE_ADMIN') {
    return 'admin';
  }
  return undefined;
}

export function resolveAccountRole(
  roleCandidate: unknown,
  organizerNamesCandidate: unknown,
  fallback: AccountRole = 'user',
): AccountRole {
  const normalizedRole = normalizeRole(roleCandidate);
  if (normalizedRole) {
    return normalizedRole;
  }
  if (Array.isArray(organizerNamesCandidate) && organizerNamesCandidate.length > 0) {
    return 'organizer';
  }
  return fallback;
}

export function buildUserFromResponse(data: AuthApiResponse, existing?: User | null): User {
  return {
    username: data.username,
    email: data.email,
    uid: existing?.uid ?? data.username,
    displayName: existing?.displayName ?? data.username,
    photoURL: existing?.photoURL,
    role: resolveAccountRole(data.roles?.[0], existing?.organizerNames),
    organizerNames: existing?.organizerNames,
  };
}

export function getCurrentUser(): User | null {
  if (currentUser) {
    return currentUser;
  }

  const storage = getStorage();
  const raw = storage?.getItem(USER_KEY);
  if (!raw) {
    return null;
  }

  try {
    currentUser = JSON.parse(raw) as User;
    return currentUser;
  } catch {
    storage?.removeItem(USER_KEY);
    return null;
  }
}

export async function loginWithEmail(email: string, password: string): Promise<AuthUser> {
  await ensureCsrfToken();
  const response = await apiCall(`${BACKEND_URL}${API_AUTH_LOGIN}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
    skipAuthErrorHandling: true,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as Record<string, unknown>;
    if (response.status === 401) {
      clearAuthState();
    }
    throw createHttpError(
      response.status,
      (body['message'] as string | undefined) ?? response.statusText,
    );
  }

  const data = await response.json() as AuthApiResponse;
  setCsrfToken(data.csrfToken);
  storeTokenExpiry(data.accessTokenExpiresInMs);
  const user = buildUserFromResponse(data);
  setCurrentUser(user);
  notifyListeners(user);
  return user;
}

export async function signupWithEmail({ username, email, password, role, confirmationToken, organizerNames }: SignupInput): Promise<AuthUser> {
  await ensureCsrfToken();
  const response = await apiCall(`${BACKEND_URL}${confirmationToken ? API_AUTH_REGISTER_WITH_KEY : API_AUTH_REGISTER}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(confirmationToken ? { username, email, password, confirmationToken } : { username, email, password }),
    skipAuthErrorHandling: true,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as Record<string, unknown>;
    throw createHttpError(
      response.status,
      (body['message'] as string | undefined) ?? response.statusText,
    );
  }

  const data = await response.json() as AuthApiResponse;
  setCsrfToken(data.csrfToken);
  storeTokenExpiry(data.accessTokenExpiresInMs);
  const user = {
    ...buildUserFromResponse(data),
    role: resolveAccountRole(data.roles?.[0], undefined, role ?? 'user'),
    organizerNames: organizerNames ? [...organizerNames] : undefined,
  };
  setCurrentUser(user);
  notifyListeners(user);
  return user;
}


export async function refreshSession(): Promise<boolean> {
  try {
    const response = await apiCall(`${BACKEND_URL}${API_AUTH_REFRESH}`, {
      method: 'POST',
      skipAuthErrorHandling: true,
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        // Clear local state but do not redirect - public pages must remain
        // accessible without authentication. Protected routes handle their own
        // redirect via route guards. A 403 here means the refresh token was
        // rejected (e.g. DB reset, token family compromised), not a CSRF attack.
        clearAuthState();
      }
      return false;
    }

    const data = await response.json() as AuthApiResponse;
    setCsrfToken(data.csrfToken);
    if (typeof data.accessTokenExpiresInMs === 'number') {
      storeTokenExpiry(data.accessTokenExpiresInMs);
    }
    const user = buildUserFromResponse(data, getCurrentUser());
    setCurrentUser(user);
    notifyListeners(user);
    return true;
  } catch {
    return false;
  }
}

export async function logout(): Promise<void> {
  try {
    await apiCall(`${BACKEND_URL}${API_AUTH_LOGOUT}`, {
      method: 'POST',
      skipAuthErrorHandling: true,
    });
  } catch {
    // Local state should still be cleared if the network request fails.
  }
  clearAuthState();
}

export function getStoredAccountRole(uid: string): AccountRole {
  const user = getCurrentUser();
  if (!user || (uid && user.uid !== uid)) {
    return 'user';
  }
  return user.role ?? 'user';
}

export function getStoredOrganizerNames(uid: string): string[] {
  const user = getCurrentUser();
  if (!user || (uid && user.uid !== uid)) {
    return [];
  }
  return Array.isArray(user.organizerNames) ? [...user.organizerNames] : [];
}

export async function getAccountProfile(uid?: string): Promise<{ role: AccountRole; organizerNames: string[] }> {
  const user = getCurrentUser();
  if (!user || (uid && user.uid !== uid)) {
    return { role: 'user', organizerNames: [] };
  }

  const fallbackRole = resolveAccountRole(user.role, user.organizerNames);
  const fallbackOrganizerNames = Array.isArray(user.organizerNames) ? [...user.organizerNames] : [];

  const response = await apiCall(`${BACKEND_URL}${API_AUTH_PROFILE}`);

  if (!response.ok) {
    return { role: fallbackRole, organizerNames: fallbackOrganizerNames };
  }

  const data = await response.json() as { role?: string; organizerNames?: string[] };
  const organizerNames = Array.isArray(data.organizerNames) ? data.organizerNames : fallbackOrganizerNames;
  const profile = {
    role: resolveAccountRole(data.role, organizerNames, fallbackRole),
    organizerNames,
  };

  const updatedUser: User = {
    ...user,
    role: profile.role,
    organizerNames: [...profile.organizerNames],
  };
  setCurrentUser(updatedUser);
  notifyListeners(updatedUser);

  return profile;
}

export function _resetForTesting(): void {
  resetCsrfTokenForTesting();
  currentUser = null;
  tokenExpiresAt = null;
  listeners.length = 0;
  getStorage()?.removeItem(USER_KEY);
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function mapAuthError(error: unknown, _context?: AuthErrorContext): string {
    if (error && typeof error === 'object') {
        const e = error as { status?: number; message?: string };
        if (e.status === 401 || e.status === 403) {
            return 'Invalid email or password.';
        }
        if (e.status === 409 || (e.status !== undefined && e.message && e.message.toLowerCase().includes('already'))) {
            return sanitizeErrorMessage(e.message ?? 'Account already exists.');
        }
        if (e.status === 400) {
            return sanitizeErrorMessage(e.message ?? 'Invalid input. Please check your details.');
        }
        // Only surface the message when it came from our backend (has a known status code).
        if (e.status !== undefined && e.message) {
            return sanitizeErrorMessage(e.message);
        }
    }
    return 'Something went wrong. Please try again.';
}

/**
 * Verify an organizer invitation key and receive a confirmation token
 * Used in Step 1 of organizer registration flow
 *
 * @param key - The organizer key (32-char alphanumeric string)
 * @returns Promise resolving to verification response with confirmation token
 * @throws HttpError with status 404, 410, or 401 for various failure cases
 */
export async function verifyOrganizerKey(key: string): Promise<OrganizerKeyVerifyResponse> {
    const response = await fetch(`${BACKEND_URL}/api/auth/organizer-key/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key }),
    });

    if (!response.ok) {
        const message = await response.text();
        throw createHttpError(response.status, message);
    }

    return response.json() as Promise<OrganizerKeyVerifyResponse>;
}

/**
 * Complete organizer registration using confirmation token from key verification
 * Used in Step 2 of organizer registration flow
 *
 * @param data - Registration request with confirmation token, username, password, email
 * @returns Promise resolving to AuthUser (with tokens, role='organizer')
 * @throws HttpError with status 401, 409, or 422 for various failure cases
 */
export async function registerOrganizerWithKey(
    data: OrganizerRegisterWithKeyRequest
): Promise<AuthUser> {
  await ensureCsrfToken();
  const response = await apiCall(`${BACKEND_URL}/api/auth/register-with-key`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
    skipAuthErrorHandling: true,
    });

    if (!response.ok) {
    const body = await response.json().catch(() => ({})) as Record<string, unknown>;
    const message = (body['message'] as string | undefined) ?? response.statusText;
        throw createHttpError(response.status, message);
    }

  const responseData = await response.json() as AuthApiResponse;
  setCsrfToken(responseData.csrfToken);
  storeTokenExpiry(responseData.accessTokenExpiresInMs);
  const user = buildUserFromResponse(responseData);
  setCurrentUser(user);
    notifyListeners(user);
    return user;
}

/**
 * Map HTTP errors to user-friendly messages for organizer key endpoints
 * Sanitizes all messages to prevent XSS attacks
 */
export function mapOrganizerKeyError(error: unknown): string {
    if (!(error instanceof Error) || !('status' in error)) {
        return 'Something went wrong. Please try again.';
    }

    const httpError = error as Error & { status: number };
    const message = (httpError.message || '').toLowerCase();

    // Verify endpoint errors
    if (httpError.status === 404) {
        return sanitizeErrorMessage('Organizer key not found. Please check and try again.');
    }
    if (httpError.status === 410) {
        return sanitizeErrorMessage('This organizer key has already been used.');
    }
    if (httpError.status === 401 && message.includes('expired')) {
        return sanitizeErrorMessage('Organizer key has expired. Please request a new one.');
    }

    // Registration endpoint errors
    if (httpError.status === 409 && message.includes('username')) {
        return sanitizeErrorMessage('This username is already taken. Please choose another.');
    }
    if (httpError.status === 409 && message.includes('email')) {
        return sanitizeErrorMessage('This email is already registered.');
    }
    if (httpError.status === 401 && message.includes('token')) {
        return sanitizeErrorMessage('Confirmation token is invalid or expired. Please verify the key again.');
    }
    if (httpError.status === 422) {
        return sanitizeErrorMessage('This confirmation link has already been used. Please verify the key again.');
    }

    // Fallback to existing mapAuthError for other cases
    return mapAuthError(error);
}

export async function submitOrganizerKeyRequest(
    data: OrganizerKeyRequestData,
): Promise<OrganizerKeyRequestResponse> {
  const response = await apiCall(`${BACKEND_URL}/api/auth/organizer-key-request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
    skipAuthErrorHandling: true,
    });

    if (!response.ok) {
    const body = await response.json().catch(() => ({})) as Record<string, unknown>;
    const message = (body['message'] as string | undefined) ?? response.statusText;
        throw createHttpError(response.status, message);
    }

    return response.json() as Promise<OrganizerKeyRequestResponse>;
}

export function mapOrganizerKeyRequestError(error: unknown): string {
    if (!(error instanceof Error) || !('status' in error)) {
        return 'Unable to submit your request right now. Please try again.';
    }

    const httpError = error as Error & { status: number };

    if (httpError.status === 400) {
        return sanitizeErrorMessage('Please check the form fields and try again.');
    }
    if (httpError.status === 409) {
        return sanitizeErrorMessage('A request with this email is already pending.');
    }
    if (httpError.status === 429) {
        return sanitizeErrorMessage('Too many requests. Please wait and try again later.');
    }

    return mapAuthError(error);
}

/**
 * Generate a new organizer invitation key and send it by email
 * Admin-only endpoint
 */
export async function generateOrganizerKey(
    data: GenerateOrganizerKeyRequest,
): Promise<GenerateOrganizerKeyResponse> {
  const response = await apiCall(`${BACKEND_URL}/api/auth/organizer-key/generate`, {
        method: 'POST',
    headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
    skipAuthErrorHandling: true,
    });

    if (!response.ok) {
    const body = await response.json().catch(() => ({})) as Record<string, unknown>;
    const message = (body['message'] as string | undefined) ?? response.statusText;
        throw createHttpError(response.status, message);
    }

    return response.json() as Promise<GenerateOrganizerKeyResponse>;
}

/**
 * Map HTTP errors to user-friendly messages for admin organizer key generation
 */
export function mapAdminKeyError(error: unknown): string {
    if (!(error instanceof Error) || !('status' in error)) {
        return sanitizeErrorMessage('Something went wrong. Please try again.');
    }

    const httpError = error as Error & { status: number };
    const message = (httpError.message || '').toLowerCase();

    if (httpError.status === 401) {
        return sanitizeErrorMessage('You are not logged in. Please log in to continue.');
    }
    if (httpError.status === 403) {
        return sanitizeErrorMessage('You do not have permission to generate invitation keys. Admin role required.');
    }
    if (httpError.status === 400) {
        if (message.includes('email')) {
            return sanitizeErrorMessage('Please enter a valid email address.');
        }
        return sanitizeErrorMessage('Invalid input. Please check your details.');
    }
    if (httpError.status === 409 && message.includes('registered')) {
        return sanitizeErrorMessage('This email is already registered. Please use a different email.');
    }
    if (httpError.status === 500 && message.includes('email')) {
        return sanitizeErrorMessage('Failed to send invitation email. Please try again later.');
    }
    if (httpError.status === 500) {
        return sanitizeErrorMessage('Server error. Please try again later.');
    }

    return sanitizeErrorMessage('An unexpected error occurred. Please try again.');
}