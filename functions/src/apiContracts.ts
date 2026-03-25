import type { StoredEvent, StoredPage } from './types';

// These types define the backend API surface independently from Express.
// The same conceptual contract is mirrored on the frontend so both sides stay aligned.
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export type EndpointStatus = 'active' | 'planned';

export type EndpointKey =
  | 'facebook.callback'
  | 'ingest.manual'
  | 'tokens.refresh'
  | 'health.check'
  | 'pages.list'
  | 'events.list'
  | 'events.getById'
  | 'events.manualSubmit';

export interface EndpointContract<K extends EndpointKey = EndpointKey> {
  key: K;
  method: HttpMethod;
  path: string;
  status: EndpointStatus;
  description: string;
}

export interface CallbackQuery {
  code: string;
  debug?: '0' | '1';
}

export interface IngestPageResult {
  pageId: string;
  pageName: string;
  status: 'success' | 'failed' | 'skipped';
  eventsProcessed?: number;
  eventsFailed?: number;
  reason?: string;
  error?: string;
  duration: number;
}

export interface IngestManualResponse {
  totalPages: number;
  totalEvents: number;
  totalEventsFailed: number;
  duration: number;
  pageResults: IngestPageResult[];
}

export interface RefreshTokensResponse {
  status: 'ok';
}

export interface HealthResponse {
  status: 'ok';
  service: 'unievent-backend';
  timestamp: string;
}

export interface PagesListResponse {
  pages: StoredPage[];
}

export interface EventsListQuery {
  pageId?: string;
  limit?: string;
  cursor?: string;
}

export interface EventsListResponse {
  events: StoredEvent[];
}

export interface EventByIdParams {
  id: string;
}

export interface EventByIdResponse {
  event: StoredEvent | null;
}

export interface ManualSubmitRequestBody {
  pageId?: string;
  title: string;
  description?: string;
  startTime: string;
  endTime?: string;
  coverImageUrl?: string;
  eventURL?: string;
}

export interface ManualSubmitResponse {
  status: 'accepted' | 'ok';
  id: string;
}

export interface EndpointRequestMap {
  // Request payload typing is keyed by endpoint name so handlers and clients can reference one contract.
  'facebook.callback': {
    params: Record<string, never>;
    query: CallbackQuery;
    body: Record<string, never>;
  };
  'ingest.manual': {
    params: Record<string, never>;
    query: Record<string, never>;
    body: Record<string, never>;
  };
  'tokens.refresh': {
    params: Record<string, never>;
    query: Record<string, never>;
    body: Record<string, never>;
  };
  'health.check': {
    params: Record<string, never>;
    query: Record<string, never>;
    body: Record<string, never>;
  };
  'pages.list': {
    params: Record<string, never>;
    query: Record<string, never>;
    body: Record<string, never>;
  };
  'events.list': {
    params: Record<string, never>;
    query: EventsListQuery;
    body: Record<string, never>;
  };
  'events.getById': {
    params: EventByIdParams;
    query: Record<string, never>;
    body: Record<string, never>;
  };
  'events.manualSubmit': {
    params: Record<string, never>;
    query: Record<string, never>;
    body: ManualSubmitRequestBody;
  };
}

export interface EndpointResponseMap {
  'facebook.callback': string;
  'ingest.manual': IngestManualResponse;
  'tokens.refresh': RefreshTokensResponse;
  'health.check': HealthResponse;
  'pages.list': PagesListResponse;
  'events.list': EventsListResponse;
  'events.getById': EventByIdResponse;
  'events.manualSubmit': ManualSubmitResponse;
}

export type EndpointParams<K extends EndpointKey> = EndpointRequestMap[K]['params'];
export type EndpointQuery<K extends EndpointKey> = EndpointRequestMap[K]['query'];
export type EndpointBody<K extends EndpointKey> = EndpointRequestMap[K]['body'];
export type EndpointResponse<K extends EndpointKey> = EndpointResponseMap[K];

// Static route metadata lives here so route paths, methods, and status do not drift between files.
export const endpointContracts = {
  'facebook.callback': {
    key: 'facebook.callback',
    method: 'GET',
    path: '/callback',
    status: 'active',
    description: 'OAuth callback for Facebook page connection',
  },
  'ingest.manual': {
    key: 'ingest.manual',
    method: 'POST',
    path: '/ingest',
    status: 'active',
    description: 'Manually ingest events for all connected pages',
  },
  'tokens.refresh': {
    key: 'tokens.refresh',
    method: 'POST',
    path: '/refresh-tokens',
    status: 'active',
    description: 'Refresh all stored page tokens',
  },
  'health.check': {
    key: 'health.check',
    method: 'GET',
    path: '/health',
    status: 'active',
    description: 'Health endpoint for runtime readiness checks',
  },
  'pages.list': {
    key: 'pages.list',
    method: 'GET',
    path: '/pages',
    status: 'active',
    description: 'List connected pages from datastore',
  },
  'events.list': {
    key: 'events.list',
    method: 'GET',
    path: '/events',
    status: 'active',
    description: 'List normalized events from datastore',
  },
  'events.getById': {
    key: 'events.getById',
    method: 'GET',
    path: '/events/:id',
    status: 'active',
    description: 'Get one event by ID',
  },
  'events.manualSubmit': {
    key: 'events.manualSubmit',
    method: 'POST',
    path: '/events/manual',
    status: 'active',
    description: 'Manually submit an event when API access is unavailable',
  },
} as const satisfies Record<EndpointKey, EndpointContract>;
