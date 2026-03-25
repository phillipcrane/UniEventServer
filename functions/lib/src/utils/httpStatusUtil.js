"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.HttpStatusUtil = void 0;
// HTTP has standardized its status codes as messages, and this utility helps sending
// responses with those codes and messages.
const statusMessages = {
    200: 'OK',
    201: 'Created',
    204: 'No Content',
    400: 'Bad Request',
    401: 'Unauthorized',
    403: 'Forbidden',
    404: 'Not Found',
    500: 'Internal Server Error',
};
// this is the part that actually gets exported and used elsewhere
exports.HttpStatusUtil = {
    getMessage: (status) => {
        return statusMessages[status] || `Unknown status code: ${status}`;
    },
    send: (res, status, body) => {
        if (body) {
            return res.status(status).send(body);
        }
        return res.status(status).send({ message: statusMessages[status] || `Unknown status code: ${status}` });
    }
};
