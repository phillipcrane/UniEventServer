import { getAuthToken } from './auth';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL ?? '';

export function buildFacebookLoginUrl(): string {
    const FB_APP_ID = import.meta.env.VITE_FACEBOOK_APP_ID as string;
    const BACKEND_BASE_URL = BACKEND_URL || window.location.origin;
    const FB_REDIRECT_URI = encodeURIComponent(`${BACKEND_BASE_URL}/api/facebook/callback`);
    const FB_SCOPES = ['pages_show_list', 'pages_read_engagement'].join(',');
    return `https://www.facebook.com/v25.0/dialog/oauth?client_id=${FB_APP_ID}&redirect_uri=${FB_REDIRECT_URI}&scope=${FB_SCOPES}`;
}

export async function getFacebookAuthUrl(): Promise<string> {
    const token = getAuthToken();
    if (!token) throw new Error('You must be logged in to connect Facebook.');

    const response = await fetch(`${BACKEND_URL}/api/facebook/auth`, {
        headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw new Error((body['message'] as string | undefined) ?? 'Failed to get Facebook auth URL');
    }

    const data = await response.json() as { url: string; state: string };
    return data.url;
}
