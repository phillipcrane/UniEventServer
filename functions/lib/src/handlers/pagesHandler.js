"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleListPages = handleListPages;
const utils_1 = require("../utils");
// Returns all connected pages currently stored in the local datastore.
async function handleListPages(_req, res, deps) {
    try {
        // This endpoint returns datastore records as-is; frontend mapping happens in the DAL.
        const pages = await deps.dataStoreService.getPages();
        const payload = { pages };
        utils_1.HttpStatusUtil.send(res, 200, payload);
    }
    catch (e) {
        utils_1.HttpStatusUtil.send(res, 500, { error: e?.message || String(e) });
    }
}
