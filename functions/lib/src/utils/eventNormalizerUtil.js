"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.normalizeEvent = normalizeEvent;
// this helpful little util converts ("normalizes") FbEventResponse, i.e. how Facebook sends
// us event data, into StoredEvent, i.e. how we store event data locally
function normalizeEvent(pageId, fbEvent) {
    return {
        id: fbEvent.id,
        pageId,
        title: fbEvent.name,
        description: fbEvent.description,
        startTime: fbEvent.start_time,
        endTime: fbEvent.end_time ?? null,
        place: fbEvent.place ?? null,
        coverImageUrl: fbEvent.cover?.source ?? null,
        eventURL: `https://facebook.com/events/${fbEvent.id}`,
    };
}
