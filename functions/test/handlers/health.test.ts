import { describe, it, expect, vi } from 'vitest';
import type { Response } from 'express';
import { handleHealth } from '../../src/handlers/healthHandler';

function mockRes() {
  return {
    status: vi.fn().mockReturnThis(),
    send: vi.fn().mockReturnThis(),
    json: vi.fn().mockReturnThis(),
  } as unknown as Response;
}

describe('handleHealth', () => {
  it('returns service health payload', async () => {
    const req: any = {};
    const res = mockRes();

    await handleHealth(req, res);

    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({
      status: 'ok',
      service: 'unievent-backend',
      timestamp: expect.any(String),
    });
  });
});
