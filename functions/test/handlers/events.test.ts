import { describe, it, expect, vi, beforeEach } from 'vitest';
import { handleGetEventById, handleListEvents, handleManualSubmitEvent } from '../../src/handlers/eventsHandler';

function mockRes() {
  return {
    status: vi.fn().mockReturnThis(),
    send: vi.fn().mockReturnThis(),
    json: vi.fn().mockReturnThis(),
  };
}

describe('handleListEvents', () => {
  let deps: any;
  let req: any;
  let res: any;

  beforeEach(() => {
    deps = {
      dataStoreService: {
        getEvents: vi.fn(),
        getEventById: vi.fn(),
        addManualEvent: vi.fn(),
      },
      facebookService: {},
      secretManagerService: {},
      storageService: {},
    };

    req = { query: {} };
    res = mockRes();
  });

  it('returns events from datastore', async () => {
    deps.dataStoreService.getEvents.mockResolvedValue([
      { id: 'e1', pageId: 'p1', title: 'Event One', startTime: new Date().toISOString() },
    ]);

    await handleListEvents(req, res, deps);

    expect(deps.dataStoreService.getEvents).toHaveBeenCalledWith({ pageId: undefined, limit: undefined });
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({
      events: [{ id: 'e1', pageId: 'p1', title: 'Event One', startTime: expect.any(String) }],
    });
  });

  it('supports page and limit query filters', async () => {
    req = { query: { pageId: 'p1', limit: '5' } };
    deps.dataStoreService.getEvents.mockResolvedValue([]);

    await handleListEvents(req, res, deps);

    expect(deps.dataStoreService.getEvents).toHaveBeenCalledWith({ pageId: 'p1', limit: 5 });
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({ events: [] });
  });

  it('returns one event by id', async () => {
    req = { params: { id: 'e1' } };
    deps.dataStoreService.getEventById.mockResolvedValue({
      id: 'e1',
      pageId: 'p1',
      title: 'Event One',
      startTime: new Date().toISOString(),
    });

    await handleGetEventById(req, res, deps);

    expect(deps.dataStoreService.getEventById).toHaveBeenCalledWith('e1');
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({
      event: {
        id: 'e1',
        pageId: 'p1',
        title: 'Event One',
        startTime: expect.any(String),
      },
    });
  });

  it('returns null when event id is not found', async () => {
    req = { params: { id: 'missing' } };
    deps.dataStoreService.getEventById.mockResolvedValue(null);

    await handleGetEventById(req, res, deps);

    expect(deps.dataStoreService.getEventById).toHaveBeenCalledWith('missing');
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({ event: null });
  });

  it('validates required manual event fields', async () => {
    req = { body: { startTime: '2026-03-24T10:00:00.000Z' } };

    await handleManualSubmitEvent(req, res, deps);

    expect(deps.dataStoreService.addManualEvent).not.toHaveBeenCalled();
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.send).toHaveBeenCalledWith({ error: 'title is required' });
  });

  it('creates manual event and returns generated id', async () => {
    req = {
      body: {
        title: 'Manual Event',
        startTime: '2026-03-24T10:00:00.000Z',
        description: 'Submitted by organizer',
      },
    };
    deps.dataStoreService.addManualEvent.mockResolvedValue({ id: 'generated-id' });

    await handleManualSubmitEvent(req, res, deps);

    expect(deps.dataStoreService.addManualEvent).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Manual Event',
        startTime: '2026-03-24T10:00:00.000Z',
      })
    );
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.send).toHaveBeenCalledWith({ status: 'ok', id: 'generated-id' });
  });
});
