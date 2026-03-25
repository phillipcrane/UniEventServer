import { promises as fs } from 'fs';
import path from 'path';
import { randomUUID } from 'crypto';
import { normalizeEvent } from '../utils';
import type { FbEventResponse, StoredEvent, StoredPage } from '../types';

type LocalDatabase = {
  pages: Record<string, StoredPage>;
  events: Record<string, any>;
};

export class DataStoreService {
  private readonly dbPath: string;

  constructor(dbPath?: string) {
    this.dbPath = dbPath ?? path.join(process.cwd(), '.data', 'db.json');
  }

  private async ensureDbFile(): Promise<void> {
    // The datastore is file-backed; we lazily create the file so local dev needs no manual bootstrap.
    const dir = path.dirname(this.dbPath);
    await fs.mkdir(dir, { recursive: true });
    try {
      await fs.access(this.dbPath);
    } catch {
      const initial: LocalDatabase = { pages: {}, events: {} };
      await fs.writeFile(this.dbPath, JSON.stringify(initial, null, 2), 'utf8');
    }
  }

  private async readDb(): Promise<LocalDatabase> {
    await this.ensureDbFile();
    const raw = await fs.readFile(this.dbPath, 'utf8');
    const parsed = JSON.parse(raw || '{}') as Partial<LocalDatabase>;
    return {
      pages: parsed.pages ?? {},
      events: parsed.events ?? {},
    };
  }

  private async writeDb(db: LocalDatabase): Promise<void> {
    await this.ensureDbFile();
    await fs.writeFile(this.dbPath, JSON.stringify(db, null, 2), 'utf8');
  }

  async addPage(pageId: string, pageData: any): Promise<void> {
    const db = await this.readDb();
    db.pages[pageId] = {
      ...(db.pages[pageId] ?? ({} as StoredPage)),
      ...(pageData as StoredPage),
      id: pageId,
    };
    await this.writeDb(db);
  }

  async updatePage(pageId: string, data: Record<string, any>): Promise<void> {
    const db = await this.readDb();
    db.pages[pageId] = {
      ...(db.pages[pageId] ?? ({ id: pageId } as StoredPage)),
      ...data,
      id: pageId,
    };
    await this.writeDb(db);
  }

  async getPages(): Promise<(StoredPage & { id: string })[]> {
    const db = await this.readDb();
    return Object.entries(db.pages).map(([id, value]) => ({
      ...value,
      id,
    }));
  }

  async getEvents(options?: { pageId?: string; limit?: number }): Promise<StoredEvent[]> {
    const db = await this.readDb();
    const events = Object.values(db.events) as StoredEvent[];

    // Filtering and sorting happen in-memory because the datastore is a single local JSON document.
    const filtered = options?.pageId
      ? events.filter((event) => event.pageId === options.pageId)
      : events;

    const sortedByStartTime = [...filtered].sort((a, b) => {
      const aTime = Date.parse(a.startTime || '') || 0;
      const bTime = Date.parse(b.startTime || '') || 0;
      return aTime - bTime;
    });

    if (typeof options?.limit === 'number' && options.limit > 0) {
      return sortedByStartTime.slice(0, options.limit);
    }

    return sortedByStartTime;
  }

  async getEventById(eventId: string): Promise<StoredEvent | null> {
    const db = await this.readDb();
    const event = db.events[eventId] as StoredEvent | undefined;
    return event ?? null;
  }

  async addManualEvent(input: {
    pageId?: string;
    title: string;
    description?: string;
    startTime: string;
    endTime?: string;
    coverImageUrl?: string;
    eventURL?: string;
  }): Promise<{ id: string }> {
    const db = await this.readDb();
    const nowIso = new Date().toISOString();
    // Manual submissions get a generated id and are marked in raw.source to distinguish them from Facebook-ingested events.
    const id = randomUUID();

    db.events[id] = {
      id,
      pageId: input.pageId || 'manual',
      title: input.title,
      description: input.description ?? null,
      startTime: input.startTime,
      endTime: input.endTime ?? null,
      coverImageUrl: input.coverImageUrl ?? null,
      eventURL: input.eventURL,
      createdAt: nowIso,
      updatedAt: nowIso,
      raw: {
        source: 'manual',
      },
    } as StoredEvent;

    await this.writeDb(db);
    return { id };
  }

  async addEvents(pageId: string, events: (FbEventResponse & { coverImageUrl?: string })[]): Promise<{ upserted: number }> {
    const currentTimeInIso = new Date().toISOString();
    const db = await this.readDb();

    for (const event of events) {
      // Existing records are updated in place so repeated ingests behave like upserts.
      const normalizedData = normalizeEvent(pageId, event);
      const existing = db.events[event.id] as any;
      db.events[event.id] = {
        ...existing,
        ...normalizedData,
        coverImageUrl: event.coverImageUrl ?? normalizedData.coverImageUrl,
        createdAt: existing?.createdAt ?? currentTimeInIso,
        updatedAt: currentTimeInIso,
      };
    }

    await this.writeDb(db);
    return { upserted: events.length };
  }
}
