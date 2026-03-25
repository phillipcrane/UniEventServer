import { describe, it, expect, vi, beforeEach } from 'vitest';
import { handleListPages } from '../../src/handlers/pagesHandler';

function mockRes() {
  return {
    status: vi.fn().mockReturnThis(),
    send: vi.fn().mockReturnThis(),
    json: vi.fn().mockReturnThis(),
  };
}

describe('handleListPages', () => {
  let deps: any;
  let req: any;
  let res: any;

  beforeEach(() => {
    deps = {
      dataStoreService: {
        getPages: vi.fn(),
      },
      facebookService: {},
      secretManagerService: {},
      storageService: {},
    };

    req = {};
    res = mockRes();
  });

  it('returns pages from datastore', async () => {
    deps.dataStoreService.getPages.mockResolvedValue([
      { id: 'p1', name: 'Page One', active: true, url: 'https://facebook.com/p1' },
    ]);

    await handleListPages(req, res, deps);

    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({
      pages: [{ id: 'p1', name: 'Page One', active: true, url: 'https://facebook.com/p1' }],
    });
  });

  it('returns 500 when datastore throws', async () => {
    deps.dataStoreService.getPages.mockRejectedValue(new Error('db unavailable'));

    await handleListPages(req, res, deps);

    expect(res.status).toHaveBeenCalledWith(500);
    expect(res.send).toHaveBeenCalledWith({ error: 'db unavailable' });
  });
});
