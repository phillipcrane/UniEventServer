import type { FbEventResponse, LongLivedToken, FacebookPage } from '../types';

export interface IFacebookService {
  getShortLivedToken(code: string): Promise<string>;
  getLongLivedToken(shortLivedToken: string): Promise<LongLivedToken>;
  getPagesFromUser(userAccessToken: string): Promise<FacebookPage[]>;
  getPageEvents(pageId: string, pageAccessToken: string): Promise<FbEventResponse[]>;
  refreshPageToken(pageToken: string): Promise<LongLivedToken>;
}

export interface ISecretManagerService {
  addPageToken(pageId: string, token: string, expiresIn: number): Promise<void>;
  updatePageToken(pageId: string, token: string, expiresIn: number): Promise<void>;
  getPageToken(pageId: string): Promise<string | null>;
  checkTokenExpiry(pageId: string): Promise<boolean>;
  markTokenExpired(pageId: string): Promise<void>;
}

export interface IStorageService {
  addImage(filePath: string, data: Buffer, contentType: string): Promise<string>;
  addImageFromUrl(filePath: string, sourceUrl: string): Promise<string>;
  getImage(filePath: string): Promise<string | null>;
  removeImage(filePath: string): Promise<void>;
}