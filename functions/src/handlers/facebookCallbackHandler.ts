import { HttpStatusUtil } from '../utils';
import { Request, Response } from 'express'; // just the express res/req types, not Express
import type { Dependencies } from '../utils';

// callback = sent back from Facebook after user authorizes app with a "code" in the URL.
// Because we're sending stuff over http, we also need to send back http responses (res)
// and input the recieved code from the HTTP request (req).

// This handler calls db, fb and secretManagerService (injected via deps) to exchange code for 
// tokens, get pages, and store tokens. Its async so it can await each step and handle errors.

export async function handleCallback(deps: Dependencies, req: Request, res: Response) {
  // we convert deps into individual consts ("destructuring") for ez access
  const { facebookService, secretManagerService, storageService, dataStoreService } = deps;

  // 1. get code from HTTP request
  const code = String(req.query.code || '').trim();
  if (!code) {
    return HttpStatusUtil.send(res, 400, 'Missing code');
  }

  try {
    // 2. exchange code for tokens and pages
    const shortLivedToken = await facebookService.getShortLivedToken(code);

    // 3. exchange short-lived token for long-lived token
    const longLivedToken = await facebookService.getLongLivedToken(shortLivedToken);

    // 4. get pages using long-lived token
    const pages = await facebookService.getPagesFromUser(longLivedToken.accessToken);

    if (!Array.isArray(pages) || pages.length === 0) {
      return HttpStatusUtil.send(res, 200, 'No pages returned.');
    }
  
    for (const page of pages) {
      try {
          // 5. store page token in Secret Manager
          console.log(`[CALLBACK] Storing token for page ${page.id}...`);
          await secretManagerService.addPageToken(page.id, page.accessToken, longLivedToken.expiresIn);
          console.log(`[CALLBACK] Token stored successfully for page ${page.id}`);
   
          // 6. store page metadata in local datastore
          const expiresInSeconds = longLivedToken.expiresIn || 5184000; // default to 60 days
          const tokenExpiresAtIso = new Date(Date.now() + expiresInSeconds * 1000).toISOString();
          const tokenExpiresInDays = Math.ceil(expiresInSeconds / (60 * 60 * 24));
          const nowIso = new Date().toISOString();
          console.log(`[CALLBACK] Storing page metadata for ${page.id}...`);
          await dataStoreService.addPage(page.id, {
            id: page.id,
            name: page.name,
            active: true,
            url: `https://facebook.com/${page.id}`,
            connectedAt: nowIso,
            tokenRefreshedAt: nowIso,
            tokenStoredAt: nowIso,
            tokenExpiresAt: tokenExpiresAtIso,
            tokenExpiresInDays,
            tokenStatus: 'valid',
            lastRefreshSuccess: true,
          });
          console.log(`[CALLBACK] Page metadata stored successfully for ${page.id}`);
      } catch (e: any) {
        console.error(`[CALLBACK] Failed to store page ${page.id}:`, e.message, e.stack);
        throw e;
      }
    }
    // 7. success! 
    res.send(`Stored ${pages.length} page token(s).`);
  
  } catch (err: any) {
    // fail...
    const msg = err.message || 'Facebook auth failed';
 
    if (String(req.query.debug) === '1') {
        return HttpStatusUtil.send(res, 500, msg);
    }
    HttpStatusUtil.send(res, 500, 'Facebook auth failed');
  }
}
