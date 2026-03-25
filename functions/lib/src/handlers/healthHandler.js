"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleHealth = handleHealth;
const utils_1 = require("../utils");
// Lightweight readiness endpoint for uptime checks and CI smoke probes.
async function handleHealth(_req, res) {
    // Keep the payload intentionally small so it stays useful for probes and smoke tests.
    const payload = {
        status: 'ok',
        service: 'unievent-backend',
        timestamp: new Date().toISOString(),
    };
    utils_1.HttpStatusUtil.send(res, 200, payload);
}
