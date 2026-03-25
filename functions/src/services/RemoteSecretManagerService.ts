import axios, { AxiosInstance } from 'axios';
import { config } from '../utils';
import type { ISecretManagerService } from './contracts';

export class RemoteSecretManagerService implements ISecretManagerService {
  private readonly http: AxiosInstance;

  constructor(baseUrl?: string) {
    this.http = axios.create({
      baseURL: baseUrl ?? config.services.secretManagerUrl,
      timeout: 10000,
    });
  }

  async addPageToken(pageId: string, token: string, expiresIn: number): Promise<void> {
    await this.http.post(`/api/secrets/pages/${pageId}/token`, { token, expiresIn });
  }

  async updatePageToken(pageId: string, token: string, expiresIn: number): Promise<void> {
    await this.http.put(`/api/secrets/pages/${pageId}/token`, { token, expiresIn });
  }

  async getPageToken(pageId: string): Promise<string | null> {
    try {
      const { data } = await this.http.get<{ token: string }>(`/api/secrets/pages/${pageId}/token`);
      return data?.token ?? null;
    } catch (error: any) {
      if (error?.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  async checkTokenExpiry(pageId: string): Promise<boolean> {
    const { data } = await this.http.get<{ expired?: boolean }>(`/api/secrets/pages/${pageId}/token/status`);
    return Boolean(data?.expired);
  }

  async markTokenExpired(pageId: string): Promise<void> {
    // UniEventServer secret-manager-service currently has no explicit expire endpoint.
    // Keep this method as a no-op in remote mode to preserve interface compatibility.
    void pageId;
  }
}