import express, { RequestHandler } from 'express';

import { endpointContracts, type EndpointKey } from './apiContracts';

// Each endpoint key gets the static contract metadata plus the concrete Express handler.
type EndpointBinding = {
  [K in EndpointKey]: (typeof endpointContracts)[K] & {
    handler: RequestHandler;
  };
}[EndpointKey];

type EndpointBindingMap = {
  [K in EndpointKey]: (typeof endpointContracts)[K] & {
    handler: RequestHandler;
  };
};

type ActiveEndpointHandlerMap = {
  'facebook.callback': RequestHandler;
  'ingest.manual': RequestHandler;
  'tokens.refresh': RequestHandler;
  'health.check': RequestHandler;
  'pages.list': RequestHandler;
  'events.list': RequestHandler;
  'events.getById': RequestHandler;
  'events.manualSubmit': RequestHandler;
};

function unwiredHandler(endpointKey: EndpointKey): RequestHandler {
  return (_req, res) => {
    res.status(500).json({ error: 'Handler not wired', endpoint: endpointKey });
  };
}

function notImplementedHandler(endpointKey: EndpointKey): RequestHandler {
  return (_req, res) => {
    res.status(501).json({
      error: 'Not implemented',
      endpoint: endpointKey,
    });
  };
}

// This is the single source of truth for route registration defaults.
// Active endpoints start as unwired and are swapped with real handlers in withActiveHandlers.
const endpointBindingsByKey: EndpointBindingMap = {
  'facebook.callback': {
    ...endpointContracts['facebook.callback'],
    handler: unwiredHandler('facebook.callback'),
  },
  'ingest.manual': {
    ...endpointContracts['ingest.manual'],
    handler: unwiredHandler('ingest.manual'),
  },
  'tokens.refresh': {
    ...endpointContracts['tokens.refresh'],
    handler: unwiredHandler('tokens.refresh'),
  },
  'health.check': {
    ...endpointContracts['health.check'],
    handler: notImplementedHandler('health.check'),
  },
  'pages.list': {
    ...endpointContracts['pages.list'],
    handler: notImplementedHandler('pages.list'),
  },
  'events.list': {
    ...endpointContracts['events.list'],
    handler: notImplementedHandler('events.list'),
  },
  'events.getById': {
    ...endpointContracts['events.getById'],
    handler: notImplementedHandler('events.getById'),
  },
  'events.manualSubmit': {
    ...endpointContracts['events.manualSubmit'],
    handler: notImplementedHandler('events.manualSubmit'),
  },
};

export const endpointBindings: EndpointBinding[] = Object.values(endpointBindingsByKey);

export type EndpointHandlerMap = ActiveEndpointHandlerMap;

export function withActiveHandlers(handlers: EndpointHandlerMap): EndpointBinding[] {
  // We keep registration data centralized, then inject the runtime handlers here.
  const configured: EndpointBindingMap = {
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

export function bindEndpoints(app: express.Express, bindings: EndpointBinding[]) {
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
