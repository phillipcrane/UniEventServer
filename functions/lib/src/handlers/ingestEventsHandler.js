"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ingestEvents = ingestEvents;
exports.handleManualIngest = handleManualIngest;
exports.handleScheduledIngest = handleScheduledIngest;
const utils_1 = require("../utils");
// this handler "ingests" i.e. gets Facebook events and stores them in the local datastore.
// the function can be called manually or automatically (scheduled)
async function ingestEvents(deps) {
    // we convert deps into individual consts ("destructuring") for ez access
    const { facebookService, secretManagerService, storageService, dataStoreService } = deps;
    const startTime = Date.now();
    // 1. get pages from datastore
    const pages = await dataStoreService.getPages();
    if (!pages || pages.length === 0) {
        return { totalPages: 0, totalEvents: 0, duration: Date.now() - startTime };
    }
    let totalEventsProcessed = 0;
    let totalEventsFailed = 0;
    const pageResults = [];
    for (const page of pages) {
        const pageStartTime = Date.now();
        try {
            // 2. get page token from Secret Manager
            const token = await secretManagerService.getPageToken(page.id);
            if (!token) {
                pageResults.push({
                    pageId: page.id,
                    pageName: page.name,
                    status: 'skipped',
                    reason: 'no_token',
                    duration: Date.now() - pageStartTime,
                });
                continue;
            }
            // 3. get events from Facebook
            const events = await facebookService.getPageEvents(page.id, token);
            if (!events || events.length === 0) {
                pageResults.push({
                    pageId: page.id,
                    pageName: page.name,
                    status: 'success',
                    eventsProcessed: 0,
                    eventsFailed: 0,
                    duration: Date.now() - pageStartTime,
                });
                continue;
            }
            const eventsData = [];
            let pageEventsFailed = 0;
            for (const event of events) {
                try {
                    // 4. store event cover image in local image storage
                    let coverImageUrl = event.cover?.source;
                    if (coverImageUrl) {
                        try {
                            // store cover image in covers/{pageId}/{eventId}
                            coverImageUrl = await storageService.addImageFromUrl(`covers/${page.id}/${event.id}`, coverImageUrl);
                            // success!
                        }
                        catch (e) {
                            // fail; just use original Facebook URL
                        }
                    }
                    // 5. normalize and prepare event data for datastore
                    eventsData.push({
                        ...event,
                        coverImageUrl,
                    });
                }
                catch (error) {
                    pageEventsFailed++;
                    totalEventsFailed++;
                }
            }
            // 6. add events to datastore
            await dataStoreService.addEvents(page.id, eventsData);
            totalEventsProcessed += eventsData.length;
            pageResults.push({
                pageId: page.id,
                pageName: page.name,
                status: 'success',
                eventsProcessed: eventsData.length,
                eventsFailed: pageEventsFailed,
                duration: Date.now() - pageStartTime,
            });
        }
        catch (error) {
            pageResults.push({
                pageId: page.id,
                pageName: page.name,
                status: 'failed',
                error: error.message || String(error),
                duration: Date.now() - pageStartTime,
            });
        }
    }
    const totalDuration = Date.now() - startTime;
    const result = {
        totalPages: pages.length,
        totalEvents: totalEventsProcessed,
        totalEventsFailed,
        duration: totalDuration,
        pageResults,
    };
    return result;
}
async function handleManualIngest(req, res, deps) {
    // currently the manual ingest runs over HTTP, but could be changed to CLI command
    try {
        const result = await ingestEvents(deps);
        utils_1.HttpStatusUtil.send(res, 200, result);
    }
    catch (e) {
        utils_1.HttpStatusUtil.send(res, 500, { error: e.message });
    }
}
async function handleScheduledIngest(event, context, deps) {
    // this function is triggered by a schedule (cron-like invocation)
    try {
        const result = await ingestEvents(deps);
        return result;
    }
    catch (error) {
        // silently fail
    }
}
