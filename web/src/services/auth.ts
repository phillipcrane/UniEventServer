const BACKEND_URL = import.meta.env.VITE_BACKEND_URL ?? '';
const USER_KEY = 'unievent_user';
const TOKEN_EXPIRES_AT_KEY = 'unievent_token_expires_at';
const CSRF_COOKIE_NAME = 'csrf_token';

// In-memory CSRF token, populated on login/register/refresh.
// On page reload it falls back to reading the readable CSRF cookie directly.
let _csrfToken: string | null = null;

export type AuthUser = {
    username: string;
    email: string;
    uid?: string;
    displayName?: string;
    photoURL?: string | null;
    role?: AccountRole;
    organizerNames?: string[];
};

export type AccountRole = 'user' | 'organizer';

type SignupInput = {
    username: string;
    email: string;
    password: string;
    role?: AccountRole;
    organizerNames?: string[];
};

type AuthErrorContext = 'login' | 'signup' | 'general';

type HttpError = Error & { status: number };

function createHttpError(status: number, message: string): HttpError {
    return Object.assign(new Error(message), { status });
}

// Module-level listener list for auth state subscriptions
const listeners: Array<(user: AuthUser | null) => void> = [];

function notifyListeners(user: AuthUser | null): void {
    listeners.forEach((cb) => cb(user));
}

function normalizeRole(value: unknown): AccountRole | undefined {
    if (typeof value !== 'string') return undefined;
    const normalized = value.trim().toUpperCase();
    if (normalized === 'ORGANIZER' || normalized === 'ROLE_ORGANIZER') return 'organizer';
    if (normalized === 'USER' || normalized === 'ROLE_USER') return 'user';
    return undefined;
}

function resolveAccountRole(
    roleCandidate: unknown,
    organizerNamesCandidate: unknown,
    fallback: AccountRole = 'user',
): AccountRole {
    const normalizedRole = normalizeRole(roleCandidate);
    if (normalizedRole) return normalizedRole;

    if (Array.isArray(organizerNamesCandidate) && organizerNamesCandidate.length > 0) {
        return 'organizer';
    }

    return fallback;
}

function getCsrfFromCookie(): string | null {
    if (typeof document === 'undefined') return null;
    const match = document.cookie.split('; ').find(row => row.startsWith(`${CSRF_COOKIE_NAME}=`));
    return match ? decodeURIComponent(match.slice(CSRF_COOKIE_NAME.length + 1)) : null;
}

export function getCsrfToken(): string | null {
    return _csrfToken ?? getCsrfFromCookie();
}

function persistUser(user: AuthUser): void {
    localStorage.setItem(USER_KEY, JSON.stringify({
        username: user.username,
        email: user.email,
        uid: user.uid,
        displayName: user.displayName,
        photoURL: user.photoURL,
        role: user.role,
        organizerNames: user.organizerNames,
    }));
}

function clearUser(): void {
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(TOKEN_EXPIRES_AT_KEY);
}

function storeTokenExpiry(accessTokenExpiresInMs: number): void {
    localStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(Date.now() + accessTokenExpiresInMs));
}

export function getTokenExpiresAt(): number | null {
    const raw = localStorage.getItem(TOKEN_EXPIRES_AT_KEY);
    return raw ? Number(raw) : null;
}

export function isTokenExpiredOrExpiringSoon(thresholdMs = 60_000): boolean {
    const expiresAt = getTokenExpiresAt();
    if (expiresAt === null) return false;
    return Date.now() >= expiresAt - thresholdMs;
}

export function getCurrentUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
        const stored = JSON.parse(raw) as {
            username: string;
            email: string;
            uid?: string;
            displayName?: string;
            photoURL?: string | null;
            role?: AccountRole;
            organizerNames?: string[];
        };

        const organizerNames = Array.isArray(stored.organizerNames) ? stored.organizerNames : undefined;

        return {
            username: stored.username,
            email: stored.email,
            uid: stored.uid ?? stored.username,
            displayName: stored.displayName ?? stored.username,
            photoURL: stored.photoURL,
            role: resolveAccountRole(stored.role, organizerNames),
            organizerNames,
        };
    } catch {
        return null;
    }
}

export async function loginWithEmail(email: string, password: string): Promise<AuthUser> {
    const response = await fetch(`${BACKEND_URL}/api/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw createHttpError(
            response.status,
            (body['message'] as string | undefined) ?? response.statusText,
        );
    }

    const data = await response.json() as { username: string; email: string; role: string; csrfToken: string; accessTokenExpiresInMs: number };
    _csrfToken = data.csrfToken;
    storeTokenExpiry(data.accessTokenExpiresInMs);
    const user: AuthUser = {
        username: data.username,
        email: data.email,
        uid: data.username,
        displayName: data.username,
        role: resolveAccountRole(data.role, undefined),
    };
    persistUser(user);
    notifyListeners(user);
    return user;
}

export async function signupWithEmail({ username, email, password, role, organizerNames }: SignupInput): Promise<AuthUser> {
    const response = await fetch(`${BACKEND_URL}/api/auth/register`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password }),
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw createHttpError(
            response.status,
            (body['message'] as string | undefined) ?? response.statusText,
        );
    }

    const data = await response.json() as { username: string; email: string; role: string; csrfToken: string; accessTokenExpiresInMs: number };
    _csrfToken = data.csrfToken;
    storeTokenExpiry(data.accessTokenExpiresInMs);
    const user: AuthUser = {
        username: data.username,
        email: data.email,
        uid: data.username,
        displayName: data.username,
        role: resolveAccountRole(data.role, undefined) ?? role,
        organizerNames: organizerNames ? [...organizerNames] : undefined,
    };
    persistUser(user);
    notifyListeners(user);
    return user;
}

export function onAuthUserChanged(callback: (user: AuthUser | null) => void): () => void {
    listeners.push(callback);
    // fire immediately with current state
    callback(getCurrentUser());
    return () => {
        const idx = listeners.indexOf(callback);
        if (idx !== -1) listeners.splice(idx, 1);
    };
}

export async function refreshTokens(): Promise<void> {
    const csrf = getCsrfToken();
    const response = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...(csrf ? { 'X-CSRF-Token': csrf } : {}),
        },
    });

    if (!response.ok) {
        // Refresh failed - session has fully expired, log out locally.
        _csrfToken = null;
        clearUser();
        notifyListeners(null);
        return;
    }

    const data = await response.json() as { username: string; email: string; role: string; csrfToken: string; accessTokenExpiresInMs: number };
    _csrfToken = data.csrfToken;
    storeTokenExpiry(data.accessTokenExpiresInMs);

    const current = getCurrentUser();
    if (current) {
        const updated: AuthUser = {
            ...current,
            role: resolveAccountRole(data.role, current.organizerNames),
        };
        persistUser(updated);
        notifyListeners(updated);
    }
}

export async function signOutCurrentUser(): Promise<void> {
    const csrf = getCsrfToken();
    try {
        await fetch(`${BACKEND_URL}/api/auth/logout`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...(csrf ? { 'X-CSRF-Token': csrf } : {}),
            },
        });
    } catch {
        // ignore network errors - local state is cleared regardless
    }
    _csrfToken = null;
    clearUser();
    notifyListeners(null);
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

    const response = await fetch(`${BACKEND_URL}/api/auth/profile`, {
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
    });

    if (!response.ok) {
        return { role: fallbackRole, organizerNames: fallbackOrganizerNames };
    }

    const data = await response.json() as { role?: string; organizerNames?: string[] };
    const profileOrganizerNames = Array.isArray(data.organizerNames) ? data.organizerNames : fallbackOrganizerNames;
    const profile = {
        role: resolveAccountRole(data.role, profileOrganizerNames, fallbackRole),
        organizerNames: profileOrganizerNames,
    };

    const updatedUser: AuthUser = {
        ...user,
        role: profile.role,
        organizerNames: [...profile.organizerNames],
    };
    persistUser(updatedUser);
    notifyListeners(updatedUser);

    return profile;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function mapAuthError(error: unknown, _context?: AuthErrorContext): string {
    if (error && typeof error === 'object') {
        const e = error as { status?: number; message?: string };
        if (e.status === 401 || e.status === 403) {
            return 'Invalid email or password.';
        }
        if (e.status === 409 || (e.status !== undefined && e.message && e.message.toLowerCase().includes('already'))) {
            return e.message ?? 'Account already exists.';
        }
        if (e.status === 400) {
            return e.message ?? 'Invalid input. Please check your details.';
        }
        // Only surface the message when it came from our backend (has a known status code).
        if (e.status !== undefined && e.message) {
            return e.message;
        }
    }
    return 'Something went wrong. Please try again.';
}
