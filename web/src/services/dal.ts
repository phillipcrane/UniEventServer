/**
 * Data Access Layer (DAL)
 *
 * Communicates with the UniEventServer backend at /api/events, /api/pages, etc.
 */

import { API_AUTH_ORGANIZER_KEY_VERIFY, BACKEND_URL } from '../constants';
import type { ApiResponse, Event, EventApiResponse, OrganizerKeyVerification, Page, PageApiResponse } from '../types';
import { apiCall } from './http';

function buildBackendUrl(path: string): URL {
  return new URL(path, BACKEND_URL || window.location.origin);
}

function buildBackendUrlString(path: string): string {
  return buildBackendUrl(path).toString();
}


/**
 * Map backend EventApiResponse to frontend Event type
 */
function mapEventResponse(data: EventApiResponse): Event {
  return {
    id: data.id,
    pageId: data.pageId,
    title: data.title,
    description: data.description,
    startTime: data.startTime,
    endTime: data.endTime,
    place: data.place,
    coverImageUrl: data.coverImageId
      ? new URL(`/media/${data.coverImageId}`, BACKEND_URL || window.location.origin).toString()
      : undefined,
    eventURL: data.eventUrl,
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
  };
}

/**
 * Map backend PageApiResponse to frontend Page type
 */
function mapPageResponse(data: PageApiResponse): Page {
  return {
    id: data.id,
    name: data.name,
    url: data.url,
    active: data.active,
  };
}

async function createFetchError(response: Response, context: string): Promise<Error> {
  const statusDetails = response.statusText
    ? `${response.status} ${response.statusText}`
    : `${response.status}`;

  let message = `${context}: ${statusDetails}`;

  try {
    const bodyText = await response.text();

    if (bodyText) {
      try {
        const parsed: unknown = JSON.parse(bodyText);
        if (
          typeof parsed === 'object' &&
          parsed !== null &&
          'message' in parsed &&
          typeof parsed.message === 'string' &&
          parsed.message.trim() !== ''
        ) {
          message = `${message} - ${parsed.message}`;
        } else if (bodyText.trim() !== '') {
          message = `${message} - ${bodyText}`;
        }
      } catch {
        if (bodyText.trim() !== '') {
          message = `${message} - ${bodyText}`;
        }
      }
    }
  } catch {
    // Ignore body parsing failures and fall back to status-based message.
  }

  return new Error(message);
}

/**
 * Fetch all pages (paginated, defaults to first 100)
 */
export async function getPages(page: number = 0, size: number = 100): Promise<Page[]> {
  const url = buildBackendUrl('/api/pages');
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch pages');
  }

  const data: ApiResponse<PageApiResponse> = await response.json();
  return data.content.map(mapPageResponse);
}

/**
 * Fetch active pages only (paginated)
 */
export async function getActivePages(page: number = 0, size: number = 100): Promise<Page[]> {
  const url = buildBackendUrl('/api/pages/active');
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch active pages');
  }

  const data: ApiResponse<PageApiResponse> = await response.json();
  return data.content.map(mapPageResponse);
}

/**
 * Search pages by name (case-insensitive, paginated)
 */
export async function searchPages(query: string, page: number = 0, size: number = 100): Promise<Page[]> {
  const url = buildBackendUrl('/api/pages/search');
  url.searchParams.append('name', query);
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to search pages');
  }

  const data: ApiResponse<PageApiResponse> = await response.json();
  return data.content.map(mapPageResponse);
}

/**
 * Fetch all events (paginated, defaults to first 100, sorted by startTime)
 */
export async function getEvents(page: number = 0, size: number = 100): Promise<Event[]> {
  const url = buildBackendUrl('/api/events');
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());
  url.searchParams.append('sort', 'startTime,asc');

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch events');
  }

  const data: ApiResponse<EventApiResponse> = await response.json();
  return data.content.map(mapEventResponse);
}

/**
 * Fetch upcoming events only (startTime >= now, paginated)
 */
export async function getFutureEvents(page: number = 0, size: number = 100): Promise<Event[]> {
  const url = buildBackendUrl('/api/events/future');
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch future events');
  }

  const data: ApiResponse<EventApiResponse> = await response.json();
  return data.content.map(mapEventResponse);
}

/**
 * Fetch a single event by ID
 */
export async function getEventById(id: string): Promise<Event | null> {
  const url = buildBackendUrlString(`/api/events/${id}`);

  const response = await apiCall(url);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch event');
  }

  const data: EventApiResponse = await response.json();
  return mapEventResponse(data);
}

/**
 * Fetch events for a specific page/organizer
 */
export async function getEventsByPageId(pageId: string, page: number = 0, size: number = 100): Promise<Event[]> {
  const url = buildBackendUrl(`/api/events/page/${pageId}`);
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch events for page');
  }

  const data: ApiResponse<EventApiResponse> = await response.json();
  return data.content.map(mapEventResponse);
}

/**
 * Fetch future events for a specific page/organizer
 */
export async function getFutureEventsByPageId(pageId: string, page: number = 0, size: number = 100): Promise<Event[]> {
  const url = buildBackendUrl(`/api/events/page/${pageId}/future`);
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch future events for page');
  }

  const data: ApiResponse<EventApiResponse> = await response.json();
  return data.content.map(mapEventResponse);
}

/**
 * Fetch events for a specific place/venue
 */
export async function getEventsByPlaceId(placeId: string, page: number = 0, size: number = 100): Promise<Event[]> {
  const url = buildBackendUrl(`/api/events/place/${placeId}`);
  url.searchParams.append('page', page.toString());
  url.searchParams.append('size', size.toString());

  const response = await apiCall(url.toString());
  if (!response.ok) {
    throw await createFetchError(response, 'Failed to fetch events for place');
  }

  const data: ApiResponse<EventApiResponse> = await response.json();
  return data.content.map(mapEventResponse);
}

/**
 * Verify an organizer invite key against the backend.
 * Returns true if the key is valid, false if the backend rejects it.
 * Throws on network error.
 */
export async function verifyOrganizerKey(key: string): Promise<OrganizerKeyVerification | null> {
  const url = buildBackendUrlString(API_AUTH_ORGANIZER_KEY_VERIFY);
  const response = await apiCall(url, {
    method: 'POST',
    body: JSON.stringify({ key }),
    skipAuthErrorHandling: true,
  });
  if (!response.ok) {
    return null;
  }
  return response.json() as Promise<OrganizerKeyVerification>;
}
