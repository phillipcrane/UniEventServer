"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataStoreService = void 0;
const fs_1 = require("fs");
const path_1 = __importDefault(require("path"));
const crypto_1 = require("crypto");
const utils_1 = require("../utils");
class DataStoreService {
    dbPath;
    constructor(dbPath) {
        this.dbPath = dbPath ?? path_1.default.join(process.cwd(), '.data', 'db.json');
    }
    async ensureDbFile() {
        // The datastore is file-backed; we lazily create the file so local dev needs no manual bootstrap.
        const dir = path_1.default.dirname(this.dbPath);
        await fs_1.promises.mkdir(dir, { recursive: true });
        try {
            await fs_1.promises.access(this.dbPath);
        }
        catch {
            const initial = { pages: {}, events: {} };
            await fs_1.promises.writeFile(this.dbPath, JSON.stringify(initial, null, 2), 'utf8');
        }
    }
    async readDb() {
        await this.ensureDbFile();
        const raw = await fs_1.promises.readFile(this.dbPath, 'utf8');
        const parsed = JSON.parse(raw || '{}');
        return {
            pages: parsed.pages ?? {},
            events: parsed.events ?? {},
        };
    }
    async writeDb(db) {
        await this.ensureDbFile();
        await fs_1.promises.writeFile(this.dbPath, JSON.stringify(db, null, 2), 'utf8');
    }
    async addPage(pageId, pageData) {
        const db = await this.readDb();
        db.pages[pageId] = {
            ...(db.pages[pageId] ?? {}),
            ...pageData,
            id: pageId,
        };
        await this.writeDb(db);
    }
    async updatePage(pageId, data) {
        const db = await this.readDb();
        db.pages[pageId] = {
            ...(db.pages[pageId] ?? { id: pageId }),
            ...data,
            id: pageId,
        };
        await this.writeDb(db);
    }
    async getPages() {
        const db = await this.readDb();
        return Object.entries(db.pages).map(([id, value]) => ({
            ...value,
            id,
        }));
    }
    async getEvents(options) {
        const db = await this.readDb();
        const events = Object.values(db.events);
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
    async getEventById(eventId) {
        const db = await this.readDb();
        const event = db.events[eventId];
        return event ?? null;
    }
    async addManualEvent(input) {
        const db = await this.readDb();
        const nowIso = new Date().toISOString();
        // Manual submissions get a generated id and are marked in raw.source to distinguish them from Facebook-ingested events.
        const id = (0, crypto_1.randomUUID)();
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
        };
        await this.writeDb(db);
        return { id };
    }
    async addEvents(pageId, events) {
        const currentTimeInIso = new Date().toISOString();
        const db = await this.readDb();
        for (const event of events) {
            // Existing records are updated in place so repeated ingests behave like upserts.
            const normalizedData = (0, utils_1.normalizeEvent)(pageId, event);
            const existing = db.events[event.id];
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
exports.DataStoreService = DataStoreService;
