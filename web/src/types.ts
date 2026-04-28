// "types" = data type "molds" that DB items are cast into
// "export" = make the type available to other files
// "slug" = short, stable, human-readable identifier in the URL

// N.B.: We're only using Facebook-only domain types

export interface Page { // A FB Page we pull events from
    id: string;         // FB Page ID or just a slug
    name: string;
    url: string;        // Facebook Page URL
    active: boolean;
}

export interface Location {
    street?: string;
    city?: string;
    zip?: string;
    country?: string;
    latitude?: number;
    longitude?: number;
}

export interface Event {
    id: string;                  // event id
    pageId: string;              // Page id (source)
    title: string;               // FB: "name"
    description?: string;        // FB: "description"
    startTime: string;           // ISO format; FB: "start_time"
    endTime?: string;            // ISO; FB: "end_time"
    place?: Place;               // Place (where the event is held)
    coverImageUrl?: string;      // FB: event_cover.source or cover.source
    eventURL?: string;           // e.g. https://facebook.com/events/{id}
    createdAt: string;           // ISO format (when we stored it)
    updatedAt: string;           // ISO (last sync/update)
}

export interface Place {
    id?: string;      // FB Place ID
    name?: string;    // e.g. "S-Huset, DTU Lyngby"
    location?: Location;
}

// Controls how events are ordered on the main listing page.
// "upcoming" = soonest first; "newest" = most recently added first; "all" = no future-only filter, sorted upcoming.
export type SortMode = 'upcoming' | 'newest' | 'all';

// ── Auth ───────────────────────────────────────────────────────────────────

export type AccountRole = 'user' | 'organizer';

export type User = {
    username: string;
    email: string;
    uid?: string;
    displayName?: string;
    photoURL?: string | null;
    role?: AccountRole;
    organizerNames?: string[];
};

export type SignupRequest = {
    username: string;
    email: string;
    password: string;
    role?: AccountRole;
    organizerKey?: string;
};

export type HttpError = Error & { status: number };

export type AuthApiResponse = {
    username: string;
    email: string;
    roles: string[];
    csrfToken: string;
    accessTokenExpiresInMs: number;
};

// ── DAL ───────────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface EventApiResponse {
    id: string;
    pageId: string;
    title: string;
    description?: string;
    startTime: string;
    endTime?: string;
    place?: Place;
    coverImageId?: number;
    eventUrl?: string;
    createdAt: string;
    updatedAt: string;
}

export interface PageApiResponse {
    id: string;
    name: string;
    url: string;
    active: boolean;
    pictureId?: number;
}

// ── Event Page Component Props ─────────────────────────────────────────────

export interface EventHeaderProps {
    onBack: () => void;
}

export interface EventImageProps {
    coverImageUrl: string | undefined;
    title: string;
}

export interface EventDetailsProps {
    event: Event;
}

export interface EventDescriptionProps {
    description: string | undefined;
}
