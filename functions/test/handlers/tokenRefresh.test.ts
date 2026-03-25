import { describe, it, expect, vi, beforeEach } from 'vitest';
import { handleRefreshTokens } from '../../src/handlers/tokenRefreshHandler';

describe('handleRefreshTokens (basic)', () => {
  let deps: any;

  beforeEach(() => {
    deps = {
      facebookService: {
        refreshPageToken: vi.fn(),
        getPagesFromUser: vi.fn(),
      },
      secretManagerService: { getPageToken: vi.fn(), addPageToken: vi.fn() },
      dataStoreService: {
        getPages: vi.fn(),
        updatePage: vi.fn(),
      },
      storageService: {},
    };
  });

  it('does nothing if no pages', async () => {
    deps.dataStoreService.getPages.mockResolvedValue([]);
    await handleRefreshTokens(deps);
    expect(deps.facebookService.refreshPageToken).not.toHaveBeenCalled();
    expect(deps.dataStoreService.updatePage).not.toHaveBeenCalled();
  });

  it('calls updatePage with failure on error', async () => {
    const oldDate = new Date(Date.now() - 50 * 24 * 60 * 60 * 1000).toISOString();
    deps.dataStoreService.getPages.mockResolvedValue([
      { id: 'page1', tokenRefreshedAt: oldDate }
    ]);
    deps.secretManagerService.getPageToken.mockResolvedValue('token');
    deps.facebookService.refreshPageToken.mockRejectedValue(new Error('fail'));
    await handleRefreshTokens(deps);
    expect(deps.dataStoreService.updatePage).toHaveBeenCalledWith('page1', expect.objectContaining({ lastRefreshSuccess: false }));
  });
});