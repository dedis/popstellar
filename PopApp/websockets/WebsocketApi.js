import { encodeBase64 } from 'tweetnacl-util';
import {
  JSON_RPC_VERSION, objects, actions, methods, getCurrentTime, generateId, toString64, getCurrentLao,
  signStrings, hashStrings, pubKey
} from './WebsocketUtils';
import WebsocketLink from './WebsocketLink';


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



/** send a server query asking for the creation of a LAO with a given name (String) */
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

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.CREATE);
};


/** send a server query asking for a LAO update providing a new name (String) */
export const requestUpdateLao = (name) => {
  const time = getCurrentTime();
  const currentParams = getCurrentLao().params;

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.UPDATE_PROPERTIES)
    .setName(name)
    .setLastModified(time)
    .setWitnesses(currentParams.message.data.witnesses)
    .buildJson();

  const m = _generateMessage(jsonData, currentParams.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root/' + currentParams.message.data.id, m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.UPDATE_PROPERTIES);
};


/** send a server query asking for the current state of a LAO */
export const requestStateLao = () => {
  const currentData = getCurrentLao().params.message.data;

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.STATE)
    .setId(hashStrings(currentData.organizer, currentData.creation, currentData.name))
    .setName(currentData.name)
    .setCreation(currentData.creation).setLastModified(currentData.last_modified)
    .setOrganizer(encodeBase64(currentData.organizer))
    .setWitnesses(currentData.witnesses)
    .setModificationId(0).setModificationSignature([]) // TODO modif id? modif_signatures
    .buildJson();

  const m = _generateMessage(jsonData, getCurrentLao().params.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root/' + currentData.id, m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.STATE);
};


// TODO remove?
export const requestWitnessMessage = () => {

  const jsonData = new DataBuilder()
    .setObject(objects.MESSAGE).setAction(actions.WITNESS)
    .setMessageId("Hash") // TODO
    .setSignature(signStrings("jsonData from message received")) // TODO
    .buildJson();

  const m = _generateMessage(jsonData, []);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root/' + getCurrentLao().params.message.data.id, m));

  WebsocketLink.sendRequestToServer(obj, objects.MESSAGE, actions.WITNESS);
};


/** send a server query asking for the creation of a meeting given a certain name (String), startTime (Date), optional
 *  location (String), optional end time (Date) and optional extra information (Json object) */
export const requestCreateMeeting = (name, startTime, location = "", endTime = 0, extra = {}) => {
  const time = getCurrentTime();
  const lao_id = getCurrentLao().params.message.data.id;

  let json = new DataBuilder()
    .setObject(objects.MEETING).setAction(actions.CREATE)
    .setId(hashStrings(lao_id, time, name))
    .setName(name)
    .setCreation(time).setLastModified(time)
    .setLocation(location)
    .setStartTime(startTime);

  if (typeof location === 'string' && location !== "") json.setLocation(location);
  if (Number.isInteger(endTime) && !endTime) json.setEndTime(endTime);
  if (extra !== 'undefined' && Object.keys(extra).length === 0 && extra.constructor === Object) json.setExtra(extra);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root/' + lao_id, m));

  WebsocketLink.sendRequestToServer(obj, objects.MEETING, actions.CREATE);
};


/** send a server query asking for the state of a meeting */
export const requestStateMeeting = () => {
  const currentData = getCurrentLao().params.message.data;

  let json = new DataBuilder()
    .setObject(objects.MEETING).setAction(actions.STATE)
    .setId(hashStrings(currentData.id, currentData.creation, currentData.name))
    .setName(currentData.name)
    .setCreation(currentData.creation).setLastModified(currentData.last_modified);

  if (currentData.hasOwnProperty('location')) json.setLocation(currentData.location);
  if (currentData.hasOwnProperty('end')) json.setEndTime(currentData.end);
  if (currentData.hasOwnProperty('extra')) json.setExtra(currentData.extra);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData, getCurrentLao().params.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams('/root/' + currentData.id, m));

  WebsocketLink.sendRequestToServer(obj, objects.MEETING, actions.STATE);
};
