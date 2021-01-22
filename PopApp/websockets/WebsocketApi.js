import {
  JSON_RPC_VERSION, objects, actions, methods, eventTags, getCurrentTime, toString64,
  getCurrentLao, signString, hashStrings, getPublicKey, fromString64,
} from './WebsocketUtils';
import WebsocketLink from './WebsocketLink';

/* eslint-disable no-underscore-dangle */

const ROOT_CHANNEL = '/root';

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
  // we either set the channel to /root or /root followed by the base64 encoding of the channel id
  channel: channel === ROOT_CHANNEL
    ? ROOT_CHANNEL
    : `${ROOT_CHANNEL}/${toString64(channel.slice(1 + ROOT_CHANNEL.length))}`,
  message,
});

/**
 * Generate a message object from some json data (json string) and an array of witnesses
 * (arrays of public keys)
 */
const _generateMessage = (jsonData, witness_signatures = []) => {
  const encodedJsonData = toString64(jsonData);
  const sign = signString(jsonData);

  return {
    data: encodedJsonData,
    sender: getPublicKey(),
    signature: sign,
    message_id: hashStrings(encodedJsonData, sign),
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
    this._scheduled = undefined;
    this._rollCallDescription = undefined;
    this._attendees = undefined;
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
    if (this._modSign !== 'undefined') obj.modification_signatures = this._modSign;
    if (this._location !== 'undefined') obj.location = this._location;
    if (this._start !== 'undefined') obj.start = this._start;
    if (this._end !== 'undefined') obj.end = this._end;
    if (this._extra !== 'undefined') obj.extra = this._extra;
    if (this._messageId !== 'undefined') obj.message_id = this._messageId;
    if (this._signature !== 'undefined') obj.signature = this._signature;
    if (this._scheduled !== 'undefined') obj.scheduled = this._scheduled;
    if (this._rollCallDescription !== 'undefined') obj.roll_call_description = this._rollCallDescription;
    if (this._attendees !== 'undefined') obj.attendees = this._attendees;

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

  setScheduleTime(_scheduled) { this._scheduled = _scheduled; return this; }

  setRollCallDescription(_rollCallDescription) {
    this._rollCallDescription = _rollCallDescription; return this;
  }

  setAttendees(_attendees) { this._attendees = _attendees; return this; }
}

/** Send a server query asking for the creation of a LAO with a given name (String) */
export const requestCreateLao = (name) => {
  const time = getCurrentTime();

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.CREATE)
    .setId(hashStrings(getPublicKey(), time, name))
    .setName(name)
    .setCreation(time)
    .setOrganizer(getPublicKey())
    .setWitnesses([])
    .buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(ROOT_CHANNEL, m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.CREATE);
};

/** Send a server query asking for a LAO update providing a new name (String) */
export const requestUpdateLao = (name, witnesses = undefined) => {
  const time = getCurrentTime();
  const currentParams = getCurrentLao().params;

  const jsonData = new DataBuilder()
    .setObject(objects.LAO).setAction(actions.UPDATE_PROPERTIES)
    .setId(
      hashStrings(currentParams.message.data.organizer, currentParams.message.data.creation, name),
    )
    .setName(name)
    .setLastModified(time)
    .setWitnesses(witnesses || currentParams.message.data.witnesses)
    .buildJson();

  const m = _generateMessage(jsonData, currentParams.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${currentParams.message.data.id}`, m));

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
    .setLastModified(getCurrentTime())
    .setOrganizer(currentData.organizer)
    .setWitnesses(currentData.witnesses)
    .setModificationId('') // need modification_id from storage (waiting for storage)
    .setModificationSignature([]) // need modification_signatures from storage (waiting for storage)
    .buildJson();

  const m = _generateMessage(jsonData, getCurrentLao().params.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${currentData.id}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.LAO, actions.STATE);
};

/** Send a server query asking for the creation of a meeting given a certain name (String),
 *  startTime (Date), optional location (String), optional end time (Date) and optional
 *  extra information (Json object) */
export const requestCreateMeeting = (
  name, startTime, location = undefined, endTime = undefined, extra = undefined,
) => {
  const time = getCurrentTime();
  const laoId = getCurrentLao().params.message.data.id;

  const json = new DataBuilder()
    .setObject(objects.MEETING).setAction(actions.CREATE)
    .setId(hashStrings(eventTags.MEETING, laoId, time, name))
    .setName(name)
    .setCreation(time)
    .setLocation(location)
    .setStartTime(startTime);

  if (typeof location === 'string' && location !== '') json.setLocation(location);
  if (Number.isInteger(endTime) && !endTime) json.setEndTime(endTime);
  if (
    extra !== 'undefined'
    && typeof extra === 'object'
    && Object.keys(extra).length === 0
    && extra.constructor === Object
  ) json.setExtra(extra);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${laoId}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.MEETING, actions.CREATE);
};

/** Send a server query asking for the state of a meeting */
export const requestStateMeeting = (startTime) => {
  const currentData = getCurrentLao().params.message.data;

  const json = new DataBuilder()
    .setObject(objects.MEETING).setAction(actions.STATE)
    .setId(hashStrings(eventTags.MEETING, currentData.id, currentData.creation, currentData.name))
    .setName(currentData.name)
    .setCreation(currentData.creation)
    .setLastModified(getCurrentTime())
    .setStartTime(startTime)
    .setModificationId('') // need modification_id from storage (waiting for storage)
    .setModificationSignature([]); // need modification_signatures fro storage (waiting for storage)

  if (Object.prototype.hasOwnProperty.call(currentData, 'location')) json.setLocation(currentData.location);
  if (Object.prototype.hasOwnProperty.call(currentData, 'end')) json.setEndTime(currentData.end);
  if (Object.prototype.hasOwnProperty.call(currentData, 'extra')) json.setExtra(currentData.extra);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData, getCurrentLao().params.message.witness_signatures);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${currentData.id}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.MEETING, actions.STATE);
};

/** Send a server message to acknowledge witnessing the message message (JS object) */
export const requestWitnessMessage = (message) => {
  // Note: message full message (with jsonrpc, method, ... fields) in order to be able
  // to retrieve the channel (composed of "/root/ + lao_id")
  const messageId = message.params.message.message_id;

  const jsonData = new DataBuilder()
    .setObject(objects.MESSAGE).setAction(actions.WITNESS)
    .setMessageId(messageId)
    .setSignature(signString(fromString64(messageId)))
    .buildJson();

  const m = _generateMessage(jsonData, []);
  const channel = (message.params.channel === ROOT_CHANNEL)
    ? ROOT_CHANNEL
    : fromString64(message.params.channel);
  const obj = _generateQuery(
    methods.PUBLISH,
    _generateParams(channel, m),
  );

  WebsocketLink.sendRequestToServer(obj, objects.MESSAGE, actions.WITNESS);
};

/** Send a server query asking for the creation of a roll call with a given name (String) and a
 *  given location (String). An optional start time (Number), scheduled time (Number) or
 *  description (String) can be specified */
export const requestCreateRollCall = (name, location, start = -1, scheduled = -1, description = '') => {
  const time = getCurrentTime();
  const laoId = getCurrentLao().params.message.data.id;

  const json = new DataBuilder()
    .setObject(objects.ROLL_CALL).setAction(actions.CREATE)
    .setId(hashStrings(eventTags.ROLL_CALL, toString64(laoId), time, name))
    .setName(name)
    .setCreation(time)
    .setLocation(location);

  if (start !== -1) json.setStartTime(start);
  else if (scheduled !== -1) json.setScheduleTime(scheduled);

  if (description !== '') json.setRollCallDescription(description);

  const jsonData = json.buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${laoId}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.ROLL_CALL, actions.CREATE);
};

/** handles both re/opening roll calls requests for code reuse */
const _requestRollCall = (isReopening, rollCallId, start) => {
  const rollCall = { creation: 444, name: 'r-cName' }; // TODO get roll call by id from localStorage
  const laoId = getCurrentLao().params.message.data.id;
  const startTime = start === -1 ? getCurrentTime() : start;
  const action = isReopening ? actions.REOPEN : actions.OPEN;

  const jsonData = new DataBuilder()
    .setObject(objects.ROLL_CALL).setAction(action)
    .setId(hashStrings(eventTags.ROLL_CALL, laoId, rollCall.creation, rollCall.name))
    .setStartTime(startTime)
    .buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${laoId}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.ROLL_CALL, action);
};

/** Send a server query asking for the opening of a roll call given its id (Number) and an
 * optional start time (Number). If the start time is not specified, then the current time
 * will be used instead */
export const requestOpenRollCall = (rollCallId, start = -1) => (
  _requestRollCall(false, rollCallId, start)
);

/** Send a server query asking for the reopening of a roll call given its id (Number) and an
 * optional start time (Number). If the start time is not specified, then the current time
 * will be used instead */
export const requestReopenRollCall = (rollCallId, start = -1) => (
  _requestRollCall(true, rollCallId, start)
);

/** Send a server query asking for the closing of a roll call given its id (Number) and the
 * list of attendees (Array of public keys) */
export const requestCloseRollCall = (rollCallId, attendees) => {
  const rollCall = { creation: 444, start: 555, name: 'r-cName' }; // TODO get roll call by id from localStorage
  const laoId = getCurrentLao().params.message.data.id;

  const jsonData = new DataBuilder()
    .setObject(objects.ROLL_CALL).setAction(actions.CLOSE)
    .setId(hashStrings(eventTags.ROLL_CALL, laoId, rollCall.creation, rollCall.name))
    .setStartTime(rollCall.start)
    .setEndTime(getCurrentTime())
    .setAttendees(attendees)
    .buildJson();

  const m = _generateMessage(jsonData);
  const obj = _generateQuery(methods.PUBLISH, _generateParams(`${ROOT_CHANNEL}/${laoId}`, m));

  WebsocketLink.sendRequestToServer(obj, objects.ROLL_CALL, actions.CLOSE);
};
