import axios, { AxiosInstance } from 'axios';
import { config } from '../utils';
import type { IStorageService } from './contracts';

export class RemoteStorageService implements IStorageService {
  private readonly http: AxiosInstance;

  constructor(baseUrl?: string) {
    this.http = axios.create({
      baseURL: baseUrl ?? config.services.storageUrl,
      timeout: 10000,
    });
  }

  async addImage(filePath: string, data: Buffer, contentType: string): Promise<string> {
    const { data: payload } = await this.http.post<{ mediaLink?: string; filePath?: string }>(
      '/api/storage/images',
      data,
      {
        params: { filePath, contentType },
        headers: { 'Content-Type': 'application/octet-stream' },
      }
    );

    return payload?.mediaLink || payload?.filePath || filePath;
  }

  async addImageFromUrl(filePath: string, sourceUrl: string): Promise<string> {
    const { data } = await this.http.post<{ mediaLink?: string; filePath?: string }>(
      '/api/storage/images/from-url',
      { filePath, sourceUrl }
    );
    return data?.mediaLink || data?.filePath || sourceUrl;
  }

  async getImage(filePath: string): Promise<string | null> {
    try {
      const { data } = await this.http.get<{ mediaLink?: string; exists?: boolean }>(`/api/storage/images/${filePath}`);
      if (data?.exists === false) {
        return null;
      }
      return data?.mediaLink || null;
    } catch (error: any) {
      if (error?.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  async removeImage(filePath: string): Promise<void> {
    await this.http.delete(`/api/storage/images/${filePath}`);
  }
}