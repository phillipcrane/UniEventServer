// Set env vars BEFORE any imports!
process.env.FACEBOOK_APP_ID = 'test-app-id';
process.env.FACEBOOK_APP_SECRET = 'test-app-secret';
process.env.FB_REDIRECT_URI = 'https://example.com/callback';

import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { FacebookService } from '../../src/services/FacebookService';
import type { FbShortLivedTokenResponse, FbLongLivedTokenResponse, FbPageResponse, FbEventResponse } from '../../src/types';

// Mock axios globally
vi.mock('axios');
const mockedAxios = vi.mocked(axios, true);

describe('FacebookService', () => {
  let facebookService: FacebookService;
  let mockAxiosInstance: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockAxiosInstance = { get: vi.fn() };
    mockedAxios.create = vi.fn().mockReturnValue(mockAxiosInstance);
    facebookService = new FacebookService();
  });

  it('exchanges code for short-lived token', async () => {
    const mockResponse: FbShortLivedTokenResponse = { access_token: 'short-lived-token' };
    mockAxiosInstance.get.mockResolvedValueOnce({ data: mockResponse });

    const token = await facebookService.getShortLivedToken('code123');
    expect(token).toBe('short-lived-token');
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/oauth/access_token', expect.any(Object));
  });

  it('exchanges short-lived for long-lived token', async () => {
    const mockResponse: FbLongLivedTokenResponse = { access_token: 'long-lived-token', expires_in: 1234 };
    mockAxiosInstance.get.mockResolvedValueOnce({ data: mockResponse });

    const result = await facebookService.getLongLivedToken('short-lived-token');
    expect(result.accessToken).toBe('long-lived-token');
    expect(result.expiresIn).toBe(1234);
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/oauth/access_token', expect.any(Object));
  });

  it('fetches user pages', async () => {
    const mockPages: FbPageResponse[] = [
      { id: '1', name: 'Page1', access_token: 'token1' },
      { id: '2', name: 'Page2', access_token: 'token2' },
    ];
    mockAxiosInstance.get.mockResolvedValueOnce({ data: { data: mockPages } });

    const pages = await facebookService.getPagesFromUser('user-token');
    expect(pages).toHaveLength(2);
    expect(pages[0].id).toBe('1');
    expect(pages[1].accessToken).toBe('token2');
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/me/accounts', expect.any(Object));
  });

  it('fetches page events', async () => {
    const mockEvents: FbEventResponse[] = [
      { id: 'e1', name: 'Event1', start_time: '2025-01-01T00:00:00+0000' },
    ];
    mockAxiosInstance.get.mockResolvedValueOnce({ data: { data: mockEvents } });

    const events = await facebookService.getPageEvents('pageid', 'pagetoken');
    expect(events).toHaveLength(1);
    expect(events[0].id).toBe('e1');
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/pageid/events', expect.any(Object));
  });

  it('refreshes long-lived token', async () => {
    const mockResponse: FbLongLivedTokenResponse = { access_token: 'refreshed-token', expires_in: 9999 };
    mockAxiosInstance.get.mockResolvedValueOnce({ data: mockResponse });

    const result = await facebookService.refreshLongLivedToken('old-token');
    expect(result.accessToken).toBe('refreshed-token');
    expect(result.expiresIn).toBe(9999);
  });

  it('handles API errors', async () => {
    mockAxiosInstance.get.mockRejectedValueOnce(new Error('fail'));
    await expect(facebookService.getShortLivedToken('bad')).rejects.toThrow('fail');
  });
});