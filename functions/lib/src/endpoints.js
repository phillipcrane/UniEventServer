"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.endpointBindings = void 0;
exports.withActiveHandlers = withActiveHandlers;
exports.bindEndpoints = bindEndpoints;
const apiContracts_1 = require("./apiContracts");
function unwiredHandler(endpointKey) {
    return (_req, res) => {
        res.status(500).json({ error: 'Handler not wired', endpoint: endpointKey });
    };
}
function notImplementedHandler(endpointKey) {
    return (_req, res) => {
        res.status(501).json({
            error: 'Not implemented',
            endpoint: endpointKey,
        });
    };
}
// This is the single source of truth for route registration defaults.
// Active endpoints start as unwired and are swapped with real handlers in withActiveHandlers.
const endpointBindingsByKey = {
    'facebook.callback': {
        ...apiContracts_1.endpointContracts['facebook.callback'],
        handler: unwiredHandler('facebook.callback'),
    },
    'ingest.manual': {
        ...apiContracts_1.endpointContracts['ingest.manual'],
        handler: unwiredHandler('ingest.manual'),
    },
    'tokens.refresh': {
        ...apiContracts_1.endpointContracts['tokens.refresh'],
        handler: unwiredHandler('tokens.refresh'),
    },
    'health.check': {
        ...apiContracts_1.endpointContracts['health.check'],
        handler: notImplementedHandler('health.check'),
    },
    'pages.list': {
        ...apiContracts_1.endpointContracts['pages.list'],
        handler: notImplementedHandler('pages.list'),
    },
    'events.list': {
        ...apiContracts_1.endpointContracts['events.list'],
        handler: notImplementedHandler('events.list'),
    },
    'events.getById': {
        ...apiContracts_1.endpointContracts['events.getById'],
        handler: notImplementedHandler('events.getById'),
    },
    'events.manualSubmit': {
        ...apiContracts_1.endpointContracts['events.manualSubmit'],
        handler: notImplementedHandler('events.manualSubmit'),
    },
};
exports.endpointBindings = Object.values(endpointBindingsByKey);
function withActiveHandlers(handlers) {
    // We keep registration data centralized, then inject the runtime handlers here.
    const configured = {
        ...endpointBindingsByKey,
        'facebook.callback': {
            ...endpointBindingsByKey['facebook.callback'],
            handler: handlers['facebook.callback'],
        },
        'ingest.manual': {
            ...endpointBindingsByKey['ingest.manual'],
            handler: handlers['ingest.manual'],
        },
        'tokens.refresh': {
            ...endpointBindingsByKey['tokens.refresh'],
            handler: handlers['tokens.refresh'],
        },
        'health.check': {
            ...endpointBindingsByKey['health.check'],
            handler: handlers['health.check'],
        },
        'pages.list': {
            ...endpointBindingsByKey['pages.list'],
            handler: handlers['pages.list'],
        },
        'events.list': {
            ...endpointBindingsByKey['events.list'],
            handler: handlers['events.list'],
        },
        'events.getById': {
            ...endpointBindingsByKey['events.getById'],
            handler: handlers['events.getById'],
        },
        'events.manualSubmit': {
            ...endpointBindingsByKey['events.manualSubmit'],
            handler: handlers['events.manualSubmit'],
        },
    };
    return Object.values(configured);
}
function bindEndpoints(app, bindings) {
    // Route registration is driven entirely by the contract/binding tables above.
    for (const endpoint of bindings) {
        if (endpoint.method === 'GET') {
            app.get(endpoint.path, endpoint.handler);
            continue;
        }
        if (endpoint.method === 'POST') {
            app.post(endpoint.path, endpoint.handler);
            continue;
        }
    }
}
