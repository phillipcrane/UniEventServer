import { Request, Response } from 'express';
import { HttpStatusUtil } from '../utils';
import type { EndpointResponse } from '../apiContracts';

// Lightweight readiness endpoint for uptime checks and CI smoke probes.
export async function handleHealth(_req: Request, res: Response) {
  // Keep the payload intentionally small so it stays useful for probes and smoke tests.
  const payload: EndpointResponse<'health.check'> = {
    status: 'ok',
    service: 'unievent-backend',
    timestamp: new Date().toISOString(),
  };

  HttpStatusUtil.send(res, 200, payload);
}