const BACKEND_URL = import.meta.env.VITE_BACKEND_URL ?? '';
const CSRF_COOKIE_NAME = 'csrf_token';

let _csrfToken: string | null = null;
let _currentUser: AuthUser | null = null;
let _tokenExpiresAt: number | null = null;

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

function setCurrentUser(user: AuthUser): void {
    _currentUser = user;
}

function clearCurrentUser(): void {
    _currentUser = null;
    _tokenExpiresAt = null;
}

function storeTokenExpiry(accessTokenExpiresInMs: number): void {
    _tokenExpiresAt = Date.now() + accessTokenExpiresInMs;
}

export function getTokenExpiresAt(): number | null {
    return _tokenExpiresAt;
}

export function isTokenExpiredOrExpiringSoon(thresholdMs = 60_000): boolean {
    if (_tokenExpiresAt === null) return false;
    return Date.now() >= _tokenExpiresAt - thresholdMs;
}

export function getCurrentUser(): AuthUser | null {
    return _currentUser;
}

type AuthApiResponse = {
    username: string;
    email: string;
    roles: string[];
    csrfToken: string;
    accessTokenExpiresInMs: number;
};

function buildUserFromResponse(data: AuthApiResponse, existing?: AuthUser | null): AuthUser {
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

    const data = await response.json() as AuthApiResponse;
    _csrfToken = data.csrfToken;
    storeTokenExpiry(data.accessTokenExpiresInMs);
    const user = buildUserFromResponse(data);
    setCurrentUser(user);
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

    const data = await response.json() as AuthApiResponse;
    _csrfToken = data.csrfToken;
    storeTokenExpiry(data.accessTokenExpiresInMs);
    const user: AuthUser = {
        ...buildUserFromResponse(data),
        role: resolveAccountRole(data.roles?.[0], organizerNames) ?? role,
        organizerNames: organizerNames ? [...organizerNames] : undefined,
    };
    setCurrentUser(user);
    notifyListeners(user);
    return user;
}

export function onAuthUserChanged(callback: (user: AuthUser | null) => void): () => void {
    listeners.push(callback);
    callback(_currentUser);
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
        _csrfToken = null;
        clearCurrentUser();
        notifyListeners(null);
        return;
    }

    const data = await response.json() as AuthApiResponse;
    _csrfToken = data.csrfToken;
    storeTokenExpiry(data.accessTokenExpiresInMs);
    const user = buildUserFromResponse(data, _currentUser);
    setCurrentUser(user);
    notifyListeners(user);
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
    clearCurrentUser();
    notifyListeners(null);
}

export function getStoredAccountRole(uid: string): AccountRole {
    const user = _currentUser;
    if (!user || (uid && user.uid !== uid)) {
        return 'user';
    }
    return user.role ?? 'user';
}

export function getStoredOrganizerNames(uid: string): string[] {
    const user = _currentUser;
    if (!user || (uid && user.uid !== uid)) {
        return [];
    }
    return Array.isArray(user.organizerNames) ? [...user.organizerNames] : [];
}

export async function getAccountProfile(uid?: string): Promise<{ role: AccountRole; organizerNames: string[] }> {
    const user = _currentUser;
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
    setCurrentUser(updatedUser);
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
        if (e.status !== undefined && e.message) {
            return e.message;
        }
    }
    return 'Something went wrong. Please try again.';
}

export function _resetForTesting(): void {
    _currentUser = null;
    _tokenExpiresAt = null;
    _csrfToken = null;
    listeners.length = 0;
}
