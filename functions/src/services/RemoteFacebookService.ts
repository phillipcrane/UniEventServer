import axios, { AxiosInstance } from 'axios';
import { config } from '../utils';
import type { FbEventResponse, FacebookPage, LongLivedToken } from '../types';
import type { IFacebookService } from './contracts';

export class RemoteFacebookService implements IFacebookService {
  private readonly http: AxiosInstance;

  constructor(baseUrl?: string) {
    this.http = axios.create({
      baseURL: baseUrl ?? config.services.facebookUrl,
      timeout: 10000,
    });
  }

  async getShortLivedToken(code: string): Promise<string> {
    const { data } = await this.http.post<{ access_token: string }>('/api/facebook/oauth/token', { code });
    return data.access_token;
  }

  async getLongLivedToken(shortLivedToken: string): Promise<LongLivedToken> {
    const { data } = await this.http.post<{ access_token: string; expires_in: number }>(
      '/api/facebook/oauth/long-lived-token',
      { short_lived_token: shortLivedToken }
    );
    return { accessToken: data.access_token, expiresIn: data.expires_in };
  }

  async getPagesFromUser(userAccessToken: string): Promise<FacebookPage[]> {
    const { data } = await this.http.get<Array<{ id: string; name: string; access_token: string }>>(
      '/api/facebook/user/pages',
      { params: { accessToken: userAccessToken } }
    );

    return (data || []).map((page) => ({
      id: page.id,
      name: page.name,
      accessToken: page.access_token,
    }));
  }

  async getPageEvents(pageId: string, pageAccessToken: string): Promise<FbEventResponse[]> {
    const { data } = await this.http.get<FbEventResponse[]>(`/api/facebook/pages/${pageId}/events`, {
      params: { accessToken: pageAccessToken },
    });
    return data || [];
  }

  async refreshPageToken(pageToken: string): Promise<LongLivedToken> {
    const { data } = await this.http.post<{ access_token: string; expires_in: number }>(
      '/api/facebook/oauth/refresh',
      { page_token: pageToken }
    );
    return { accessToken: data.access_token, expiresIn: data.expires_in };
  }
}