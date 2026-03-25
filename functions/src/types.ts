// The whole point of typescript is "javascript with types", so this file predefines the types
// (e.g. that str is indeed a str and http res is indeed a https res object). TS does this with 
// interfaces that you apply to objects, though note that this is just for type checking, meanwhile
// in java/c#/etc interfaces are entire contracts with methods to implement. 

// The difference is quite interesting actually: TS interfaces don't exist when running the code 
// ("runtime"), only when its compiled/built ("compile time"). They only help you while writing the
// code, so if the app is running and a var has the wrong type, TS won't help you. java and c# interfaces
// do exist when running though. This is all a long-winded way of saying that to do real type checking
// when running the app, you need to use external libraries, a popular i've used before being "zod"

// the purpose of this file is dump all the type interfaces used in the backend functions so they can
// be repurposed and imported elsewhere easily, kind of like a util function or class.

// FACEBOOK GRAPH API TYPES
// i.e. how the data looks when we get it from Facebook Graph API
export interface FbLocation {
  street?: string;
  city?: string;
  zip?: string;
  country?: string;
  latitude?: number;
  longitude?: number;
}

export interface FbPlace {
  id?: string;
  name?: string;
  location?: FbLocation;
}

export interface FbEventResponse {
  id: string;
  name: string;
  description?: string;
  start_time: string;
  end_time?: string | null;
  place?: FbPlace | null;
  cover?: { source: string } | null;
}

export interface FbPageResponse {
  id: string;
  name: string;
  access_token: string;
}

export interface FbShortLivedTokenResponse {
  access_token: string;
}

export interface FbLongLivedTokenResponse {
  access_token: string;
  expires_in: number;
}


// DATASTORE TYPES
// i.e. how data is stored in our local datastore
export interface StoredEvent {
  id: string;
  pageId: string;
  title: string;
  description?: string | null;
  startTime: string;
  endTime?: string | null;
  place?: FbPlace | null;
  coverImageUrl?: string | null;
  eventURL?: string;
  createdAt?: string;
  updatedAt?: string;
  raw?: any;
}

export interface StoredPage {
  id: string;
  name: string;
  pictureUrl: string;
  instagramId?: string;
  // token timestamps: some docs use tokenRefreshedAt (server timestamp), others use
  // tokenStoredAt/tokenExpiresAt fields. Support both shapes for backward-compatibility.
  tokenRefreshedAt?: string;
  tokenStoredAt?: string;
  tokenExpiresAt?: string;
  tokenExpiresInDays?: number;
  tokenStatus?: string;
  lastRefreshSuccess?: boolean;
  lastRefreshError?: string;
  lastRefreshAttempt?: string;
}


// SERVICE TYPES
// interfaces used by our services but not in DB
export interface PageToken {
  token: string;
  expiresAt: string; // ISO string
}

export interface LongLivedToken {
  accessToken: string;
  expiresIn: number;
}

export interface FacebookPage {
  id: string;
  name: string;
  accessToken: string;
}
