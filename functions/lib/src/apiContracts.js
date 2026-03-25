"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.endpointContracts = void 0;
// Static route metadata lives here so route paths, methods, and status do not drift between files.
exports.endpointContracts = {
    'facebook.callback': {
        key: 'facebook.callback',
        method: 'GET',
        path: '/callback',
        status: 'active',
        description: 'OAuth callback for Facebook page connection',
    },
    'ingest.manual': {
        key: 'ingest.manual',
        method: 'POST',
        path: '/ingest',
        status: 'active',
        description: 'Manually ingest events for all connected pages',
    },
    'tokens.refresh': {
        key: 'tokens.refresh',
        method: 'POST',
        path: '/refresh-tokens',
        status: 'active',
        description: 'Refresh all stored page tokens',
    },
    'health.check': {
        key: 'health.check',
        method: 'GET',
        path: '/health',
        status: 'active',
        description: 'Health endpoint for runtime readiness checks',
    },
    'pages.list': {
        key: 'pages.list',
        method: 'GET',
        path: '/pages',
        status: 'active',
        description: 'List connected pages from datastore',
    },
    'events.list': {
        key: 'events.list',
        method: 'GET',
        path: '/events',
        status: 'active',
        description: 'List normalized events from datastore',
    },
    'events.getById': {
        key: 'events.getById',
        method: 'GET',
        path: '/events/:id',
        status: 'active',
        description: 'Get one event by ID',
    },
    'events.manualSubmit': {
        key: 'events.manualSubmit',
        method: 'POST',
        path: '/events/manual',
        status: 'active',
        description: 'Manually submit an event when API access is unavailable',
    },
};
