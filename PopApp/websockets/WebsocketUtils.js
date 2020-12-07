import { getStore } from '../Store/configureStore';
import { decodeUTF8, encodeBase64} from "tweetnacl-util";
import { sign } from "tweetnacl";

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

export const getCurrentLao = () => getStore().getState().currentLaoReducer.lao;

export const toString64 = (str) => btoa(str);
export const fromString64 = (str) => atob(str);

// See https://gist.github.com/gordonbrander/2230317
export const generateId = () => parseInt(Math.random().toString(16).substr(2, 9), 16) & 0xfffffff;
export const getCurrentTime = () => Math.floor(Date.now() / 1000);


export const PendingRequest = class {
  constructor(message, requestObject, requestAction, retryCount = 0) {
    this.message = message;
    this.requestObject = requestObject;
    this.requestAction = requestAction;
    this.retryCount = retryCount;
  }
};


export const signStrings = (...strs) => {
  let str = '';
  strs.forEach((item) => str += item);

  return encodeBase64(sign(decodeUTF8(str), secKey));
};

const hashLib = require('hash.js');
export const hashStrings = (...strs) => {
  let str = '';
  strs.forEach((item) => str += item);

  return toString64(hashLib.sha256().update(str).digest('hex'));
};

/* TEMP */
const pair = sign.keyPair();
export const pubKey = pair.publicKey; // 32 bytes
const secKey = pair.secretKey; // 64 bytes

