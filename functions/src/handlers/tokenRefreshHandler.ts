import type { Dependencies } from '../utils';

export async function handleRefreshTokens(deps: Dependencies): Promise<void> {
  const { facebookService, secretManagerService, storageService, dataStoreService } = deps;

  const startTime = Date.now();

  // 1. get pages from datastore
  const pages = await dataStoreService.getPages();
  if (!pages || pages.length === 0) {
    return;
  }

  let tokensRefreshed = 0;
  let tokensFailed = 0;

  for (const page of pages) {
    try {
      // 2. get page token from Secret Manager
      const currentPageToken = await secretManagerService.getPageToken(page.id);
      if (!currentPageToken) {
        throw new Error('No token found in Secret Manager for page');
      }

      // 3. refresh long-lived token
      const refreshedToken = await facebookService.refreshPageToken(currentPageToken);

      // 4. store new page token in Secret Manager
      await secretManagerService.updatePageToken(page.id, refreshedToken.accessToken, refreshedToken.expiresIn || 5184000);

      // 5. update page metadata
      await dataStoreService.updatePage(page.id, {
        tokenRefreshedAt: new Date().toISOString(),
        lastRefreshSuccess: true,
        lastRefreshError: null,
      });
      tokensRefreshed++;
    } catch (err: any) {
      tokensFailed++;
      const errorMsg = err?.message || String(err);

      try {
        await dataStoreService.updatePage(page.id, {
          lastRefreshSuccess: false,
          lastRefreshError: errorMsg,
          lastRefreshAttempt: new Date().toISOString(),
        });
      } catch (dbErr: any) {
        // silently fail
      }
    }
  }

  return;
}
