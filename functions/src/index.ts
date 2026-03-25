import express from 'express';

import {
  handleCallback as handler_handleCallback,
	handleHealth as handler_handleHealth,
	handleManualSubmitEvent as handler_handleManualSubmitEvent,
	handleGetEventById as handler_handleGetEventById,
	handleListEvents as handler_handleListEvents,
  handleManualIngest as handler_handleManualIngest,
  handleScheduledIngest as handler_handleScheduledIngest,
  handleRefreshTokens as handler_handleRefreshTokens,
	handleListPages as handler_handleListPages,
} from './handlers';
import { bindEndpoints, withActiveHandlers } from './endpoints';
import {
	FacebookService,
	RemoteFacebookService,
	RemoteSecretManagerService,
	RemoteStorageService,
	SecretManagerService,
	StorageService,
	DataStoreService,
} from './services';
import { config } from './utils';

// Helper: build dependencies for handlers. We set runtime env vars consumed by utils/services
function buildDepsFromParams() {
	const useMicroserviceDal = config.integration.useMicroserviceDal;

	const facebookService = useMicroserviceDal
		? new RemoteFacebookService()
		: new FacebookService();
	const secretManagerService = useMicroserviceDal
		? new RemoteSecretManagerService()
		: new SecretManagerService();
	const storageService = useMicroserviceDal
		? new RemoteStorageService()
		: new StorageService();
	const dataStoreService = new DataStoreService();

	return { facebookService, secretManagerService, storageService, dataStoreService } as const;
}

// These wrappers adapt the pure handler modules to the runtime app by constructing dependencies per request.
export async function handleCallback(req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleCallback(deps as any, req, res);
		return;
	} catch (err: any) {
		res.status(500).send(err?.message || String(err));
	}
}

export async function handleManualIngest(req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleManualIngest(req, res, deps as any);
		return;
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleRefreshTokens(_req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		// The underlying refresh handler is job-oriented, so the HTTP wrapper converts success into a simple JSON ack.
		await handler_handleRefreshTokens(deps as any);
		res.json({ status: 'ok' });
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleHealth(req: express.Request, res: express.Response) {
	try {
		await handler_handleHealth(req, res);
		return;
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleListPages(req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleListPages(req, res, deps as any);
		return;
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleListEvents(req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleListEvents(req, res, deps as any);
		return;
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleGetEventById(req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleGetEventById(req, res, deps as any);
		return;
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleManualSubmitEvent(req: express.Request, res: express.Response) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleManualSubmitEvent(req, res, deps as any);
		return;
	} catch (e: any) {
		res.status(500).send(e?.message || String(e));
	}
}

export async function handleRefreshTokensScheduled(event?: any) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleRefreshTokens(deps as any);
		return;
	} catch (err: any) {
		console.error('Scheduled token refresh failed', err?.message || err);
	}
}

export async function handleScheduleIngest(event?: any) {
	const deps = buildDepsFromParams();
	try {
		await handler_handleScheduledIngest(event, {}, deps as any);
		return;
	} catch (err: any) {
		console.error('Scheduled ingest failed', err?.message || err);
	}
}

export const app = express();
app.use((req, res, next) => {
	const requestOrigin = req.headers.origin;
	const allowedOrigins = config.cors.allowedOrigins;

	if (!requestOrigin) {
		next();
		return;
	}

	if (allowedOrigins.length === 0 || allowedOrigins.includes(requestOrigin)) {
		res.header('Access-Control-Allow-Origin', requestOrigin);
		res.header('Vary', 'Origin');
		res.header('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
		res.header('Access-Control-Allow-Headers', 'Content-Type,Authorization');
	}

	if (req.method === 'OPTIONS') {
		res.status(204).send();
		return;
	}

	next();
});
// JSON parsing is required for POST /events/manual and keeps future POST endpoints consistent.
app.use(express.json());
bindEndpoints(
	app,
	// Route paths and methods come from the central endpoint registry; only live handlers are injected here.
	withActiveHandlers({
		'facebook.callback': handleCallback,
		'ingest.manual': handleManualIngest,
		'tokens.refresh': handleRefreshTokens,
		'health.check': handleHealth,
		'pages.list': handleListPages,
		'events.list': handleListEvents,
		'events.getById': handleGetEventById,
		'events.manualSubmit': handleManualSubmitEvent,
	})
);

if (require.main === module) {
	const port = Number(process.env.PORT || 8080);
	app.listen(port, () => {
		console.log(`UniEvent backend listening on port ${port}`);
	});
}
