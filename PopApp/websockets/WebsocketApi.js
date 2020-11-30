import { decodeUTF8, encodeBase64 } from 'tweetnacl-util';
import { sign } from 'tweetnacl';
import {
  JSON_RPC_VERSION, objects, actions, getCurrentTime, methods, generateId, toString64,
} from './WebsocketUtils';
import WebsocketLink from './WebsocketLink';

const hashLib = require('hash.js');

const pair = sign.keyPair();
const pubKey = pair.publicKey; // 32 bytes
const secKey = pair.secretKey; // 64 bytes

const signStrings = (...strs) => {
  let str = '';
  strs.forEach((item) => str += item);

  return encodeBase64(sign(decodeUTF8(str), secKey));
};

const hashStrings = (...strs) => {
  let str = '';
  strs.forEach((item) => str += item);

  return toString64(hashLib.sha256().update(str).digest('hex'));
};

const _generateQuery = (method, params, id) => {
  let tid = id;
  if (id === undefined) tid = generateId();

  return {
    jsonrpc: JSON_RPC_VERSION,
    method: method,
    params: params,
    id: tid,
  }
};

const _generateParams = (channel, message) => {
  return {
    channel: toString64(channel),
    message: message,
  }
};

const _generateMessage = (jsonData, witness_signatures = []) => {
  const sign = signStrings(jsonData);

  console.log(encodeBase64(pubKey));

  return {
    data: toString64(jsonData),
    sender: encodeBase64(pubKey),
    signature: sign,
    message_id: hashStrings(jsonData, sign),
    witness_signatures: witness_signatures,
  }
};


class DataBuilder {
  constructor() {
    this._object = undefined;
    this._action = undefined;
    this._id = undefined;
    this._name = undefined;
    this._creation = undefined;
    this._last_modified = undefined;
    this._organizer = undefined;
    this._witnesses = undefined;
    this._mod_id = undefined;
    this._mod_sign = undefined;
    this._location = undefined;
    this._start = undefined;
    this._end = undefined;
    this._extra = undefined;
    this._message_id = undefined;
    this._signature = undefined;
  }

  build() {
    let obj = {};

    if (this._object !== 'undefined') obj.object = this._object;
    if (this._action !== 'undefined') obj.action = this._action;
    if (this._id !== 'undefined') obj.id = this._id;
    if (this._name !== 'undefined') obj.name = this._name;
    if (this._creation !== 'undefined') obj.creation = this._creation;
    if (this._last_modified !== 'undefined') obj.last_modified = this._last_modified;
    if (this._organizer !== 'undefined') obj.organizer = this._organizer;
    if (this._witnesses !== 'undefined') obj.witnesses = this._witnesses;
    if (this._mod_id !== 'undefined') obj.modification_id = this._mod_id;
    if (this._mod_sign !== 'undefined') obj.modification_signature = this._mod_sign;
    if (this._location !== 'undefined') obj.location = this._location;
    if (this._start !== 'undefined') obj.start = this._start;
    if (this._end !== 'undefined') obj.end = this._end;
    if (this._extra !== 'undefined') obj.extra = this._extra;
    if (this._message_id !== 'undefined') obj.message_id = this._message_id;
    if (this._signature !== 'undefined') obj.signature = this._signature;

    return obj
  }

  buildJson() { return JSON.stringify(this.build()) }

  setObject(_object) { this._object = _object; return this }
  setAction(_action) { this._action = _action; return this }
  setId(_id) { this._id = _id; return this }
  setName(_name) { this._name = _name; return this }
  setCreation(_creation) { this._creation = _creation; return this }
  setLastModified(_last_modified) { this._last_modified = _last_modified; return this }
  setOrganizer(_organizer) { this._organizer = _organizer; return this }
  setWitnesses(_witnesses) { this._witnesses = _witnesses; return this }
  setModificationId(_mod_id) { this._mod_id = _mod_id; return this }
  setModificationSignature(_mod_sign) { this._mod_sign = _mod_sign; return this }
  setLocation(_location) { this._location = _location; return this }
  setStartTime(_start) { this._start = _start; return this }
  setEndTime(_end) { this._end = _end; return this }
  setExtra(_extra) { this._extra = _extra; return this }
  setMessageId(_message_id) { this._message_id = _message_id; return this }
  setSignature(_signature) { this._signature = _signature; return this }
}




export const requestCreateLao = (name) => {
  const time = getCurrentTime();

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.CREATE)
    .setId(hashStrings(pubKey, time, name))
    .setName(name)
    .setCreation(time).setLastModified(time)
    .setOrganizer(encodeBase64(pubKey))
    .setWitnesses([])
    .buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root', m));

  WebsocketLink.sendRequestToServer(obj);
};


export const requestUpdateLao = (name) => {
  const time = getCurrentTime();

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.UPDATE_PROPERTIES)
    .setName(name)
    .setLastModified(time)
    .setWitnesses([]) // TODO witnesses from LAO
    .buildJson();

  const m = _generateMessage(jsonData); // TODO witn
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root', m)); // TODO LAO ID in channel

  WebsocketLink.sendRequestToServer(obj);
};

export const requestStateLao = () => {
  const time = getCurrentTime();

  const jsonData = new DataBuilder()
    /*.setObject(objects.LAO).setAction(actions.STATE)
    .setId(hashStrings(pubKey, ???, name))
    .setName(name)
    .setCreation(time).setLastModified(time)
    .setOrganizer(encodeBase64(pubKey))
    .setWitnesses([])*/ // + add fields
    .buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root', m));

  WebsocketLink.sendRequestToServer(obj);
};

export const requestWitnessMessage = () => {};

export const requestCreateMeeting = () => {};

export const requestStateMeeting = () => {};
