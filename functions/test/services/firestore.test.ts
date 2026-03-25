import { describe, it, expect, beforeEach } from 'vitest';
import { mkdtempSync } from 'fs';
import { join } from 'path';
import { tmpdir } from 'os';
import { DataStoreService } from '../../src/services/DataStoreService';

describe('DataStoreService', () => {
  let dataStoreService: DataStoreService;

  beforeEach(() => {
    const testDir = mkdtempSync(join(tmpdir(), 'unievent-datastore-'));
    dataStoreService = new DataStoreService(join(testDir, 'db.json'));
  });

  it('adds a page', async () => {
    await dataStoreService.addPage('1', { name: 'TestPage' });
    const pages = await dataStoreService.getPages();
    expect(pages).toEqual([{ id: '1', name: 'TestPage' }]);
  });

  it('updates a page', async () => {
    await dataStoreService.addPage('1', { name: 'TestPage' });
    await dataStoreService.updatePage('1', { name: 'UpdatedPage' });
    const pages = await dataStoreService.getPages();
    expect(pages).toEqual([{ id: '1', name: 'UpdatedPage' }]);
  });

  it('gets pages', async () => {
    await dataStoreService.addPage('1', { name: 'TestPage' });
    const pages = await dataStoreService.getPages();
    expect(pages).toEqual([{ id: '1', name: 'TestPage' }]);
  });

  it('adds events', async () => {
    const result = await dataStoreService.addEvents('1', [{ id: 'e1', name: 'Event1', start_time: new Date().toISOString() } as any]);
    expect(result).toEqual({ upserted: 1 });
  });

  it('gets events with optional page filter and limit', async () => {
    await dataStoreService.addEvents('1', [
      { id: 'e1', name: 'Event1', start_time: '2026-03-23T10:00:00.000Z' } as any,
      { id: 'e2', name: 'Event2', start_time: '2026-03-23T11:00:00.000Z' } as any,
    ]);
    await dataStoreService.addEvents('2', [
      { id: 'e3', name: 'Event3', start_time: '2026-03-23T12:00:00.000Z' } as any,
    ]);

    const allEvents = await dataStoreService.getEvents();
    const pageOneEvents = await dataStoreService.getEvents({ pageId: '1' });
    const limitedEvents = await dataStoreService.getEvents({ limit: 2 });

    expect(allEvents).toHaveLength(3);
    expect(pageOneEvents).toHaveLength(2);
    expect(pageOneEvents.every((event) => event.pageId === '1')).toBe(true);
    expect(limitedEvents).toHaveLength(2);
  });

  it('gets event by id', async () => {
    await dataStoreService.addEvents('1', [
      { id: 'e1', name: 'Event1', start_time: '2026-03-23T10:00:00.000Z' } as any,
    ]);

    const found = await dataStoreService.getEventById('e1');
    const missing = await dataStoreService.getEventById('missing');

    expect(found?.id).toBe('e1');
    expect(missing).toBeNull();
  });

  it('adds manual event and persists it', async () => {
    const created = await dataStoreService.addManualEvent({
      title: 'Manual Event',
      startTime: '2026-03-24T10:00:00.000Z',
      description: 'Created by admin',
    });

    expect(created.id).toBeTruthy();

    const stored = await dataStoreService.getEventById(created.id);
    expect(stored?.title).toBe('Manual Event');
    expect(stored?.pageId).toBe('manual');
  });
});