import { encodeBase64, decodeBase64 } from 'tweetnacl-util';
import {
  JSON_RPC_VERSION, objects, actions, methods, getCurrentTime, toString64,
  getCurrentLao, signStrings, hashStrings, getPublicKey, fromString64,
} from './WebsocketUtils';
import WebsocketLink from './WebsocketLink';

/* eslint-disable no-underscore-dangle */

/** Generate a client query from a method (methods enum),
 * a params object (Json Object) and an optional id (number) */
const _generateQuery = (method, params, id) => ({
  jsonrpc: JSON_RPC_VERSION,
  method,
  params,
  id: (id === undefined) ? -1 : id,
});

/** Generate a params object from a channel (string) and a message object (Json Object) */
const _generateParams = (channel, message) => ({
  channel: toString64(channel),
  message,
});

/**
 * Generate a message object from some json data (json string) and an array of witnesses
 * (arrays of public keys)
 */
const _generateMessage = (jsonData, witness_signatures = []) => {
  const sign = signStrings(jsonData);

  return {
    data: toString64(jsonData),
    sender: getPublicKey(),
    signature: sign,
    message_id: hashStrings(jsonData, sign),
    witness_signatures,
  };
};

/** Builder for a data object */
class DataBuilder {
  constructor() {
    this._object = undefined;
    this._action = undefined;
    this._id = undefined;
    this._name = undefined;
    this._creation = undefined;
    this._lastModified = undefined;
    this._organizer = undefined;
    this._witnesses = undefined;
    this._modId = undefined;
    this._modSign = undefined;
    this._location = undefined;
    this._start = undefined;
    this._end = undefined;
    this._extra = undefined;
    this._messageId = undefined;
    this._signature = undefined;
  }

  build() {
    const obj = {};

    if (this._object !== 'undefined') obj.object = this._object;
    if (this._action !== 'undefined') obj.action = this._action;
    if (this._id !== 'undefined') obj.id = this._id;
    if (this._name !== 'undefined') obj.name = this._name;
    if (this._creation !== 'undefined') obj.creation = this._creation;
    if (this._lastModified !== 'undefined') obj.last_modified = this._lastModified;
    if (this._organizer !== 'undefined') obj.organizer = this._organizer;
    if (this._witnesses !== 'undefined') obj.witnesses = this._witnesses;
    if (this._modId !== 'undefined') obj.modification_id = this._modId;
    if (this._modSign !== 'undefined') obj.modification_signature = this._modSign;
    if (this._location !== 'undefined') obj.location = this._location;
    if (this._start !== 'undefined') obj.start = this._start;
    if (this._end !== 'undefined') obj.end = this._end;
    if (this._extra !== 'undefined') obj.extra = this._extra;
    if (this._messageId !== 'undefined') obj.message_id = this._messageId;
    if (this._signature !== 'undefined') obj.signature = this._signature;

    return obj;
  }

  buildJson() { return JSON.stringify(this.build()); }

  setObject(_object) { this._object = _object; return this; }

  setAction(_action) { this._action = _action; return this; }

  setId(_id) { this._id = _id; return this; }

  setName(_name) { this._name = _name; return this; }

  setCreation(_creation) { this._creation = _creation; return this; }

  setLastModified(_lastModified) { this._lastModified = _lastModified; return this; }

  setOrganizer(_organizer) { this._organizer = _organizer; return this; }

  setWitnesses(_witnesses) { this._witnesses = _witnesses; return this; }

  setModificationId(_modId) { this._modId = _modId; return this; }

  setModificationSignature(_modSign) { this._modSign = _modSign; return this; }

  setLocation(_location) { this._location = _location; return this; }

  setStartTime(_start) { this._start = _start; return this; }

  setEndTime(_end) { this._end = _end; return this; }

  setExtra(_extra) { this._extra = _extra; return this; }

  setMessageId(_messageId) { this._messageId = _messageId; return this; }

  setSignature(_signature) { this._signature = _signature; return this; }
}

/** Send a server query asking for the creation of a LAO with a given name (String) */
export const requestCreateLao = (name) => {
  const time = getCurrentTime();

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.CREATE)
    .setId(hashStrings(decodeBase64(getPublicKey()), time, name))
    .setName(name)
    .setCreation(time)
    .setOrganizer(getPublicKey())
    .setWitnesses([])
    .buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root', m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.CREATE);
};

/** Send a server query asking for a LAO update providing a new name (String) */
export const requestUpdateLao = (name) => {
  const time = getCurrentTime();
  const currentParams = getCurrentLao().params;

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.UPDATE_PROPERTIES)
    .setId(
      hashStrings(currentParams.message.data.organizer, currentParams.message.data.creation, name),
    )
    .setName(name)
    .setLastModified(time)
    .setWitnesses(currentParams.message.data.witnesses)
    .buildJson();

  const m = _generateMessage(jsonData, currentParams.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`/root/${currentParams.message.data.id}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.UPDATE_PROPERTIES);
};

/** Send a server query asking for the current state of a LAO */
export const requestStateLao = () => {
  const currentData = getCurrentLao().params.message.data;

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.STATE)
    .setId(hashStrings(currentData.organizer, currentData.creation, currentData.name))
    .setName(currentData.name)
    .setCreation(currentData.creation)
    .setLastModified(currentData.last_modified)
    .setOrganizer(encodeBase64(currentData.organizer))
    .setWitnesses(currentData.witnesses)
    .setModificationId('TODO modification id in base64') // TODO modification_id from storage (waiting for storage)
    .setModificationSignature([]) // TODO modification_signatures from storage (waiting for storage)
    .buildJson();

  const m = _generateMessage(jsonData, getCurrentLao().params.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`/root/${currentData.id}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.STATE);
};

/** Send a server query asking for the creation of a meeting given a certain name (String),
 *  startTime (Date), optional location (String), optional end time (Date) and optional
 *  extra information (Json object) */
export const requestCreateMeeting = (name, startTime, location = '', endTime = 0, extra = {}) => {
  const time = getCurrentTime();
  const laoId = getCurrentLao().params.message.data.id;

  const json = new DataBuilder()
    .setObject(objects.MEETING).setAction(actions.CREATE)
    .setId(hashStrings(laoId, time, name))
    .setName(name)
    .setCreation(time)
    .setLocation(location)
    .setStartTime(startTime);

  if (typeof location === 'string' && location !== '') json.setLocation(location);
  if (Number.isInteger(endTime) && !endTime) json.setEndTime(endTime);
  if (extra !== 'undefined' && Object.keys(extra).length === 0 && extra.constructor === Object) json.setExtra(extra);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`/root/${laoId}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.MEETING, actions.CREATE);
};

/** Send a server query asking for the state of a meeting */
export const requestStateMeeting = () => {
  const currentData = getCurrentLao().params.message.data;

  const json = new DataBuilder()
    .setObject(objects.MEETING).setAction(actions.STATE)
    .setId(hashStrings(currentData.id, currentData.creation, currentData.name))
    .setName(currentData.name)
    .setCreation(currentData.creation)
    .setLastModified(currentData.last_modified)
    .setModificationId('TODO modification id in base64') // TODO modification_id from storage (waiting for storage)
    .setModificationSignature([]); // TODO modification_signatures fro storage (waiting for storage)

  if (Object.prototype.hasOwnProperty.call(currentData, 'location')) json.setLocation(currentData.location);
  if (Object.prototype.hasOwnProperty.call(currentData, 'end')) json.setEndTime(currentData.end);
  if (Object.prototype.hasOwnProperty.call(currentData, 'extra')) json.setExtra(currentData.extra);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData, getCurrentLao().params.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`/root/${currentData.id}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.MEETING, actions.STATE);
};

/** Send a server message to acknowledge witnessing the message message passed as parameter */
export const requestWitnessMessage = (message) => {
  // Note: message is a mid-level message
  const messageId = message.message_id;

  const jsonData = new DataBuilder()
    .setObject(objects.MESSAGE).setAction(actions.WITNESS)
    .setMessageId(messageId)
    .setSignature(signStrings(fromString64(messageId)))
    .buildJson();

  const m = _generateMessage(jsonData, []);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`/root/${getCurrentLao().params.message.data.id}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.MESSAGE, actions.WITNESS);
};
