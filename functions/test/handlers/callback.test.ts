import { describe, it, expect, vi, beforeEach } from 'vitest';
import { handleCallback } from '../../src/handlers/facebookCallbackHandler';

function mockRes() {
  return {
    status: vi.fn().mockReturnThis(),
    send: vi.fn().mockReturnThis(),
    json: vi.fn().mockReturnThis(),
  };
}

describe('handleCallback', () => {
  let deps: any;
  let req: any;
  let res: any;

  beforeEach(() => {
    deps = {
      facebookService: {
        getShortLivedToken: vi.fn(),
        getLongLivedToken: vi.fn(),
        getPagesFromUser: vi.fn(),
      },
      secretManagerService: { addPageToken: vi.fn() },
      dataStoreService: { addPage: vi.fn() },
      storageService: {},
    };
    res = mockRes();
  });

  it('returns 400 if code is missing', async () => {
    req = { query: {} };
    await handleCallback(deps, req, res);
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.send).toHaveBeenCalledWith('Missing code');
  });

  it('returns 200 if no pages returned', async () => {
    req = { query: { code: 'abc' } };
    deps.facebookService.getShortLivedToken.mockResolvedValue('short-token');
    deps.facebookService.getLongLivedToken.mockResolvedValue({ accessToken: 'long-token', expiresIn: 1234 });
    deps.facebookService.getPagesFromUser.mockResolvedValue([]);
    await handleCallback(deps, req, res);
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith('No pages returned.');
  });

  it('stores tokens and page info for each returned page', async () => {
    req = { query: { code: 'abc' } };
    deps.facebookService.getShortLivedToken.mockResolvedValue('short-token');
    deps.facebookService.getLongLivedToken.mockResolvedValue({ accessToken: 'long-token', expiresIn: 1234 });
    deps.facebookService.getPagesFromUser.mockResolvedValue([
      { id: '1', name: 'Page1', accessToken: 'token1' },
      { id: '2', name: 'Page2', accessToken: 'token2' },
    ]);
    await handleCallback(deps, req, res);
    expect(deps.secretManagerService.addPageToken).toHaveBeenCalledTimes(2);
    expect(deps.dataStoreService.addPage).toHaveBeenCalledTimes(2);
    expect(res.send).toHaveBeenCalledWith(expect.stringContaining('Stored'));
  });
});