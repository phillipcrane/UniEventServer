import axios, { AxiosInstance } from 'axios';
import { config } from '../utils';
import type {
  // Facebook Graph API response types
  FbShortLivedTokenResponse,
  FbLongLivedTokenResponse,
  FbPageResponse,
  FbEventResponse,
  
  // our own types
  LongLivedToken,
  FacebookPage,
} from '../types';
export class FacebookService {
  private http: AxiosInstance; // axios is a simple HTTP client (or object) that does HTTP reqs
  constructor() {
    this.http = axios.create({ baseURL: `https://graph.facebook.com/${config.fb.apiVersion}` });
  }

  async getShortLivedToken(code: string): Promise<string> {
    const { data } = await this.http.get<FbShortLivedTokenResponse>('/oauth/access_token', {
      params: {
        client_id: config.fb.appId,
        redirect_uri: config.oauth.redirectUri,
        client_secret: config.fb.appSecret,
        code,
      },
    });
    return data.access_token;
  }

  async getLongLivedToken(shortLivedToken: string): Promise<LongLivedToken> {
    const { data } = await this.http.get<FbLongLivedTokenResponse>('/oauth/access_token', {
      params: {
        grant_type: 'fb_exchange_token',
        client_id: config.fb.appId,
        client_secret: config.fb.appSecret,
        fb_exchange_token: shortLivedToken,
      },
    });
    return { accessToken: data.access_token, expiresIn: data.expires_in };
  }

  async getPagesFromUser(userAccessToken: string): Promise<FacebookPage[]> {
    const { data } = await this.http.get<{ data: FbPageResponse[] }>('/me/accounts', {
      params: {
        fields: 'id,name,access_token',
        access_token: userAccessToken,
      },
    });
    return (data?.data || []).map((page: FbPageResponse) => ({
      id: page.id,
      name: page.name,
      accessToken: page.access_token,
    }));
  }
  
  async getPageEvents(pageId: string, pageAccessToken: string): Promise<FbEventResponse[]> {
    const { data } = await this.http.get<{ data: FbEventResponse[] }>(`/${pageId}/events`, {
      params: {
        time_filter: 'upcoming',
        fields: 'id,name,description,start_time,end_time,place,cover{source}',
        access_token: pageAccessToken,
      },
    });
    return data?.data || [];
  }

  async refreshPageToken(pageToken: string): Promise<LongLivedToken> {
    // Page tokens are refreshed the same way as any long-lived token
    const { data } = await this.http.get<FbLongLivedTokenResponse>('/oauth/access_token', {
      params: {
        grant_type: 'fb_exchange_token',
        client_id: config.fb.appId,
        client_secret: config.fb.appSecret,
        fb_exchange_token: pageToken,
      },
    });
    return { accessToken: data.access_token, expiresIn: data.expires_in };
  }

  async refreshLongLivedToken(longLivedToken: string): Promise<LongLivedToken> {
    return this.refreshPageToken(longLivedToken);
  }
}