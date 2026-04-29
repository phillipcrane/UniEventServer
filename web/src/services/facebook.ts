import { BACKEND_URL, FACEBOOK_APP_VERSION, FACEBOOK_SCOPES } from '../constants';
import { getCsrfToken } from './auth';
import { apiCall } from './http';

export function buildFacebookLoginUrl(): string {
    const FB_APP_ID = import.meta.env.VITE_FACEBOOK_APP_ID as string;
    const BACKEND_BASE_URL = BACKEND_URL || window.location.origin;
    const redirectUri = encodeURIComponent(`${BACKEND_BASE_URL}/api/facebook/callback`);
    return `https://www.facebook.com/${FACEBOOK_APP_VERSION}/dialog/oauth?client_id=${FB_APP_ID}&redirect_uri=${redirectUri}&scope=${FACEBOOK_SCOPES}`;
}

export async function getFacebookAuthUrl(): Promise<string> {
    const csrf = getCsrfToken();
    if (!csrf) throw new Error('You must be logged in to connect Facebook.');

    const response = await apiCall(`${BACKEND_URL}/api/facebook/auth`, {
        headers: csrf ? { 'X-CSRF-Token': csrf } : {},
    });

    if (!response.ok) {
        const body = await response.json().catch(() => ({})) as Record<string, unknown>;
        throw new Error((body['message'] as string | undefined) ?? 'Failed to get Facebook auth URL');
    }

    const data = await response.json() as { url: string; state: string };
    return data.url;
}
