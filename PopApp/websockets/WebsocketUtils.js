export const JSON_RPC_VERSION = '2.0';
export const SERVER_ANSWER_FIELD_COUNT = 3;

export const methods = Object.freeze({
  SUBSCRIBE: 'subscribe',
  UNSUBSCRIBE: 'unsubscribe',
  MESSAGE: 'message',
  CATCHUP: 'catchup',
  PUBLISH: 'publish',
});

export const objects = Object.freeze({
  LAO: 'lao',
  MESSAGE: 'message',
  MEETING: 'meeting',
});

export const actions = Object.freeze({
  CREATE: 'create',
  UPDATE_PROPERTIES: 'update_properties',
  STATE: 'state',
  WITNESS: 'witness',
});

export const toString64 = (str) => btoa(str);
export const fromString64 = (str) => atob(str);

export const getCurrentTime = () => Math.floor(Date.now() / 1000);
export const generateId = () => 124;

export const PendingRequest = class {
  constructor(message, retryCount = 0) {
    this.message = message;
    this.retryCount = retryCount;
  }
};
