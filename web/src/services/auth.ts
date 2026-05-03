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
import type { AccountRole, AuthApiResponse, SignupRequest, User } from '../types';
import { createHttpError } from '../utils/authUtils';
import { getCsrfToken, resetCsrfTokenForTesting, setCsrfToken } from './csrf';
import { apiCall } from './http';

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
      if (response.status === 401) {
        clearSessionAndRedirect();
      }
      if (response.status === 403) {
        clearSessionAndRedirect();
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
