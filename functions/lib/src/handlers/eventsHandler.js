"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleListEvents = handleListEvents;
exports.handleGetEventById = handleGetEventById;
exports.handleManualSubmitEvent = handleManualSubmitEvent;
const utils_1 = require("../utils");
// Returns events from datastore with optional page filter and result limit.
async function handleListEvents(req, res, deps) {
    try {
        // Query values arrive as strings in Express, so we normalize them before handing them to the datastore.
        const pageId = typeof req.query.pageId === 'string' ? req.query.pageId : undefined;
        const parsedLimit = Number(req.query.limit);
        const limit = Number.isFinite(parsedLimit) && parsedLimit > 0 ? parsedLimit : undefined;
        const events = await deps.dataStoreService.getEvents({ pageId, limit });
        const payload = { events };
        utils_1.HttpStatusUtil.send(res, 200, payload);
    }
    catch (e) {
        utils_1.HttpStatusUtil.send(res, 500, { error: e?.message || String(e) });
    }
}
// Returns one event by ID from datastore.
async function handleGetEventById(req, res, deps) {
    try {
        // The route requires :id, but we still guard here so the handler stays safe in tests and refactors.
        const eventId = String(req.params.id || '').trim();
        if (!eventId) {
            utils_1.HttpStatusUtil.send(res, 400, { error: 'Missing event id' });
            return;
        }
        const event = await deps.dataStoreService.getEventById(eventId);
        const payload = { event };
        utils_1.HttpStatusUtil.send(res, 200, payload);
    }
    catch (e) {
        utils_1.HttpStatusUtil.send(res, 500, { error: e?.message || String(e) });
    }
}
// Stores a manually submitted event and returns its generated id.
async function handleManualSubmitEvent(req, res, deps) {
    try {
        // We validate only the minimum required fields here; richer moderation/business rules can be layered on later.
        const body = (req.body || {});
        const title = String(body.title || '').trim();
        const startTime = String(body.startTime || '').trim();
        if (!title) {
            utils_1.HttpStatusUtil.send(res, 400, { error: 'title is required' });
            return;
        }
        if (!startTime || Number.isNaN(Date.parse(startTime))) {
            utils_1.HttpStatusUtil.send(res, 400, { error: 'startTime must be a valid ISO date' });
            return;
        }
        // The datastore owns id generation so both API and non-HTTP callers can share the same creation logic.
        const created = await deps.dataStoreService.addManualEvent({
            pageId: body.pageId,
            title,
            description: body.description,
            startTime,
            endTime: body.endTime,
            coverImageUrl: body.coverImageUrl,
            eventURL: body.eventURL,
        });
        const payload = {
            status: 'ok',
            id: created.id,
        };
        utils_1.HttpStatusUtil.send(res, 200, payload);
    }
    catch (e) {
        utils_1.HttpStatusUtil.send(res, 500, { error: e?.message || String(e) });
    }
}
