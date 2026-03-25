"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildDeps = buildDeps;
const services_1 = require("../src/services");
function buildDeps() {
    const facebookService = new services_1.FacebookService();
    const secretManagerService = new services_1.SecretManagerService();
    const storageService = new services_1.StorageService();
    const dataStoreService = new services_1.DataStoreService();
    return { facebookService, secretManagerService, storageService, dataStoreService };
}
