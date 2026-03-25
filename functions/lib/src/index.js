"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.app = void 0;
exports.handleCallback = handleCallback;
exports.handleManualIngest = handleManualIngest;
exports.handleRefreshTokens = handleRefreshTokens;
exports.handleHealth = handleHealth;
exports.handleListPages = handleListPages;
exports.handleListEvents = handleListEvents;
exports.handleGetEventById = handleGetEventById;
exports.handleManualSubmitEvent = handleManualSubmitEvent;
exports.handleRefreshTokensScheduled = handleRefreshTokensScheduled;
exports.handleScheduleIngest = handleScheduleIngest;
const express_1 = __importDefault(require("express"));
const handlers_1 = require("./handlers");
const endpoints_1 = require("./endpoints");
const services_1 = require("./services");
const utils_1 = require("./utils");
// Helper: build dependencies for handlers. We set runtime env vars consumed by utils/services
function buildDepsFromParams() {
    const useMicroserviceDal = utils_1.config.integration.useMicroserviceDal;
    const facebookService = useMicroserviceDal
        ? new services_1.RemoteFacebookService()
        : new services_1.FacebookService();
    const secretManagerService = useMicroserviceDal
        ? new services_1.RemoteSecretManagerService()
        : new services_1.SecretManagerService();
    const storageService = useMicroserviceDal
        ? new services_1.RemoteStorageService()
        : new services_1.StorageService();
    const dataStoreService = new services_1.DataStoreService();
    return { facebookService, secretManagerService, storageService, dataStoreService };
}
// These wrappers adapt the pure handler modules to the runtime app by constructing dependencies per request.
async function handleCallback(req, res) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleCallback)(deps, req, res);
        return;
    }
    catch (err) {
        res.status(500).send(err?.message || String(err));
    }
}
async function handleManualIngest(req, res) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleManualIngest)(req, res, deps);
        return;
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleRefreshTokens(_req, res) {
    const deps = buildDepsFromParams();
    try {
        // The underlying refresh handler is job-oriented, so the HTTP wrapper converts success into a simple JSON ack.
        await (0, handlers_1.handleRefreshTokens)(deps);
        res.json({ status: 'ok' });
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleHealth(req, res) {
    try {
        await (0, handlers_1.handleHealth)(req, res);
        return;
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleListPages(req, res) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleListPages)(req, res, deps);
        return;
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleListEvents(req, res) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleListEvents)(req, res, deps);
        return;
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleGetEventById(req, res) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleGetEventById)(req, res, deps);
        return;
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleManualSubmitEvent(req, res) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleManualSubmitEvent)(req, res, deps);
        return;
    }
    catch (e) {
        res.status(500).send(e?.message || String(e));
    }
}
async function handleRefreshTokensScheduled(event) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleRefreshTokens)(deps);
        return;
    }
    catch (err) {
        console.error('Scheduled token refresh failed', err?.message || err);
    }
}
async function handleScheduleIngest(event) {
    const deps = buildDepsFromParams();
    try {
        await (0, handlers_1.handleScheduledIngest)(event, {}, deps);
        return;
    }
    catch (err) {
        console.error('Scheduled ingest failed', err?.message || err);
    }
}
exports.app = (0, express_1.default)();
// JSON parsing is required for POST /events/manual and keeps future POST endpoints consistent.
exports.app.use(express_1.default.json());
(0, endpoints_1.bindEndpoints)(exports.app, 
// Route paths and methods come from the central endpoint registry; only live handlers are injected here.
(0, endpoints_1.withActiveHandlers)({
    'facebook.callback': handleCallback,
    'ingest.manual': handleManualIngest,
    'tokens.refresh': handleRefreshTokens,
    'health.check': handleHealth,
    'pages.list': handleListPages,
    'events.list': handleListEvents,
    'events.getById': handleGetEventById,
    'events.manualSubmit': handleManualSubmitEvent,
}));
if (require.main === module) {
    const port = Number(process.env.PORT || 8080);
    exports.app.listen(port, () => {
        console.log(`UniEvent backend listening on port ${port}`);
    });
}
