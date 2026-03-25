import { Request, Response } from 'express';
import { HttpStatusUtil } from '../utils';
import type { EndpointResponse } from '../apiContracts';
import type { Dependencies } from '../utils';

// Returns all connected pages currently stored in the local datastore.
export async function handleListPages(_req: Request, res: Response, deps: Dependencies) {
  try {
    // This endpoint returns datastore records as-is; frontend mapping happens in the DAL.
    const pages = await deps.dataStoreService.getPages();
    const payload: EndpointResponse<'pages.list'> = { pages };
    HttpStatusUtil.send(res, 200, payload);
  } catch (e: any) {
    HttpStatusUtil.send(res, 500, { error: e?.message || String(e) });
  }
}