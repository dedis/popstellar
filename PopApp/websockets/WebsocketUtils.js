import { decodeUTF8, encodeBase64, decodeBase64 } from 'tweetnacl-util';
import { sign } from 'tweetnacl';
import { getStore } from '../Store/configureStore';

/* global btoa, atob */ // do not touch! EsLint required comment!
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
export const toString64 = (str) => btoa(str);
/** Transform a base64 string to a regular string */
export const fromString64 = (str) => atob(str);

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
 * Sign an array of strings using the client private key
 *
 * @param strings variable number of strings to sign
 * @returns {string} base64 encoded signature over the strings using client secret key
 */
export const signStrings = (...strings) => {
  let str = '';
  strings.forEach((item) => { str += item; });

  return encodeBase64(sign(decodeUTF8(str), decodeBase64(getSecretKey())));
};

const hashLib = require('hash.js');
/**
 * Hash an array of strings using SHA-256 then convert it into a base64 string
 * @param strings variable number of strings to hash
 * @returns {string} base64 encoded SHA-256 hash of the strings
 */
export const hashStrings = (...strings) => {
  let str = '';
  strings.forEach((item) => { str += item; });

  return toString64(hashLib.sha256().update(str).digest('hex'));
};
