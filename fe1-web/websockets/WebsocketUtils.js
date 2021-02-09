import { decodeUTF8, encodeBase64, decodeBase64 } from 'tweetnacl-util';
import { sign } from 'tweetnacl';
import { sha256 } from 'js-sha256';
import { getStore } from '../Store/configureStore';
import * as b64 from 'base-64';

/* eslint-disable no-underscore-dangle */

/** JSON rpc version used by our protocol */
export const JSON_RPC_VERSION = '2.0';
/** Number of fields a server answer exactly contains */
export const SERVER_ANSWER_FIELD_COUNT = 3;

/** Enumeration of all possible "method" fields in JsonMessages */
export const methods = Object.freeze({
  SUBSCRIBE: 'subscribe',
  UNSUBSCRIBE: 'unsubscribe',
  MESSAGE: 'message',
  CATCHUP: 'catchup',
  PUBLISH: 'publish',
});

/** Enumeration of all possible "object" fields in JsonMessages */
export const objects = Object.freeze({
  LAO: 'lao',
  MESSAGE: 'message',
  MEETING: 'meeting',
  ROLL_CALL: 'roll_call',
});

/** Enumeration of all possible "action" fields in JsonMessages */
export const actions = Object.freeze({
  CREATE: 'create',
  UPDATE_PROPERTIES: 'update_properties',
  STATE: 'state',
  WITNESS: 'witness',
  OPEN: 'open',
  REOPEN: 'reopen',
  CLOSE: 'close',
});

/** Enumeration of all possible event tags used in hash creation */
export const eventTags = Object.freeze({
  MEETING: 'M',
  ROLL_CALL: 'R',
});

/** Set a new key pair for the client in the local storage */
const _createKeyPair = () => {
  const pair = sign.keyPair();
  const keys = { pubKey: encodeBase64(pair.publicKey), secKey: encodeBase64(pair.secretKey) };
  getStore().dispatch({ type: 'SET_KEYPAIR', value: keys });

  return keys;
};

/** Return the user public key (base64 string) or create it if missing */
export const getPublicKey = () => {
  const { pubKey } = getStore().getState().keypairReducer;

  // create a new keypair for the user
  if (pubKey.length === 0) return _createKeyPair().pubKey;
  return pubKey;
};

/** Return the user secret key (base64 string) or create it if missing */ // TODO how to do better?
export const getSecretKey = () => {
  const { secKey } = getStore().getState().keypairReducer;

  // create a new keypair for the user
  if (secKey.length === 0) return _createKeyPair().secKey;
  return secKey;
};

/** Return the current LAO the client is connected to */
export const getCurrentLao = () => getStore().getState().currentLaoReducer.lao;

/** Transform a string to a base64 string */
export const toString64 = (str) => b64.encode(str);
/** Transform a base64 string to a regular string */
export const fromString64 = (str) => b64.decode(str);

/** Return the current time (Number - UNIX number of seconds from 1st january 1970) */
export const getCurrentTime = () => Math.floor(Date.now() / 1000);

/** Represent an already sent query to the server on which the client is waiting for an answer */
export const PendingRequest = class {
  constructor(message, requestObject, requestAction, retryCount = 0) {
    this.message = message;
    this.requestObject = requestObject;
    this.requestAction = requestAction;
    this.retryCount = retryCount;
  }
};

/**
 * Sign a string using a private key
 *
 * @param str string to sign
 * @param secKey base64 encoded private key used for signing. If not specified, the key
 * stored in the client's localStorage will be used
 * @returns {string} base64 encoded signature over the strings using client secret key
 */
export const signString = (str, secKey = undefined) => {
  const key = (secKey === undefined) ? getSecretKey() : secKey;
  return encodeBase64(sign.detached(decodeUTF8(str), decodeBase64(key)));
};

/**
 * Escape any character '"' and '\' from a string
 *
 * @param str string to be escaped
 * @returns {string} escaped string
 */
export const escapeString = (str) => {
  let strCopy = str;
  if (typeof strCopy === 'object') { strCopy = fromString64(encodeBase64(strCopy)); }

  strCopy = strCopy.toString();
  return strCopy.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
};

/**
 * Hash an array of strings using SHA-256 then convert it into a base64 string
 * @param strings variable number of strings to hash
 * @returns {string} base64 encoded SHA-256 hash of the strings
 */
export const hashStrings = (...strings) => {
  let str = '';
  strings.forEach((item) => { str = `${str}"${escapeString(item)}",`; });
  // remove the last comma and add square brackets around
  str = `[${str.slice(0, -1)}]`;

  const hash = sha256.create();

  const bString = hash.update(str).array();
  return toString64(String.fromCharCode(...bString));
};
