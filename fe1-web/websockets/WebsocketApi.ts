import { eventTags, getCurrentLao, getCurrentTime, toString64 } from './WebsocketUtils';
import WebsocketLink from './WebsocketLink';
import { ActionType, ObjectType } from '../Model/Network/Method/Message/data/messageData';
import { Message } from '../Model/Network/Method/Message/message';
import { JsonRpcRequest } from '../Model/Network/jsonRpcRequest';
import { JsonRpcMethod } from '../Model/Network/jsonRpcMethods';
import { Publish } from '../Model/Network/Method/publish';
import { Hash } from "../Model/Objects/hash";
import { PublicKey } from "../Model/Objects/publicKey";
import { Timestamp } from "../Model/Objects/timestamp";
import { Base64Data } from "../Model/Objects/base64";
import { KeyPair } from "../Model/Objects/keyPair";

/* eslint-disable no-underscore-dangle */

const ROOT_CHANNEL = '/root';

function _generateSubchannel(subChannelPath?: string): string {
  return (subChannelPath === undefined) ? ROOT_CHANNEL : `${ROOT_CHANNEL}/${subChannelPath}`;
}


/** Send a server query asking for the creation of a LAO with a given name (String) */
export function requestCreateLao(name: string) {
  const time = getCurrentTime();

  let message = Message.fromData({
    object: ObjectType.LAO,
    action: ActionType.CREATE,
    id: Hash.fromString(KeyPair.publicKey.toString(), time.toString(), name),
    name: name,
    creation: time,
    organizer: KeyPair.publicKey,
    witnesses: [],
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(ROOT_CHANNEL, message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.LAO, ActionType.CREATE);
}

/** Send a server query asking for a LAO update providing a new name (String) */
export function requestUpdateLao(name: string, witnesses?: PublicKey[]) {
  const time = getCurrentTime();
  const currentParams = getCurrentLao().params;

  let message = Message.fromData({
    object: ObjectType.LAO,
    action: ActionType.UPDATE_PROPERTIES,
    id: Hash.fromString(currentParams.message.data.organizer, currentParams.message.data.creation, name),
    name: name,
    last_modified: time,
    witnesses: (witnesses == undefined) ? currentParams.message.data.witnesses : witnesses,
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(currentParams.message.data.id), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.LAO, ActionType.UPDATE_PROPERTIES);
}

/** Send a server query asking for the current state of a LAO */
export function requestStateLao() {
  const currentData = getCurrentLao().params.message.data;

  let message = Message.fromData({
    object: ObjectType.LAO,
    action: ActionType.STATE,
    id: Hash.fromString(currentData.organizer, currentData.creation.toString(), currentData.name),
    name: currentData.name,
    creation: currentData.creation,
    last_modified: getCurrentTime(),
    organizer: currentData.organizer,
    witnesses: currentData.witnesses,
    modification_id: '', // FIXME need modification_id from storage (waiting for storage)
    modification_signatures :[], // FIXME need modification_signatures from storage (waiting for storage)
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(currentData.id), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.LAO, ActionType.STATE);
}

/** Send a server query asking for the creation of a meeting given a certain name (String),
 *  startTime (Timestamp), optional location (String), optional end time (Timestamp) and optional
 *  extra information (Json object) */
export function requestCreateMeeting(
  name: string, startTime: Timestamp, location?: string, endTime?: Timestamp, extra?: {},
) {
  const time = getCurrentTime();
  const laoId = getCurrentLao().params.message.data.id;

  let message = Message.fromData({
    object: ObjectType.MEETING,
    action: ActionType.CREATE,
    id: Hash.fromString(eventTags.MEETING, laoId, time.toString(), name),
    name: name,
    start: startTime,
    creation: time,
    location: location,
    end: endTime,
    extra: extra,
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(laoId), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.MEETING, ActionType.CREATE);
}

/** Send a server query asking for the state of a meeting */
export function requestStateMeeting(startTime: Timestamp) {
  const currentData = getCurrentLao().params.message.data;

  let message = Message.fromData({
    object: ObjectType.MEETING,
    action: ActionType.STATE,
    id: Hash.fromString(eventTags.MEETING, currentData.id.toString(), currentData.creation.toString(), currentData.name),
    name: currentData.name,
    creation: currentData.creation,
    last_modified: getCurrentTime(),
    start: startTime,
    location: currentData.location,
    end: currentData.end,
    extra: currentData.extra,
    modification_id: '', // FIXME need modification_id from storage (waiting for storage)
    modification_signatures :[], // FIXME need modification_signatures from storage (waiting for storage)
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(currentData.id), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.MEETING, ActionType.STATE);
}

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(witnessedMessage: JsonRpcRequest) {
  // Note: message full message (with json-rpc, method, ... fields) in order to be able
  // to retrieve the channel (composed of "/root/ + lao_id")
  const messageId = witnessedMessage.params.message.message_id;

  let message = Message.fromData({
    object: ObjectType.MESSAGE,
    action: ActionType.WITNESS,
    message_id: messageId,
    signature: KeyPair.privateKey.sign(messageId),
  });

  const channel = (witnessedMessage.params.channel === ROOT_CHANNEL)
      ? ROOT_CHANNEL
      : new Base64Data(witnessedMessage.params.channel).decode();
  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(channel, message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.MESSAGE, ActionType.WITNESS);
}

/** Send a server query asking for the creation of a roll call with a given name (String) and a
 *  given location (String). An optional start time (Timestamp), scheduled time (Timestamp) or
 *  description (String) can be specified */
export function requestCreateRollCall(
    name: string, location: string, start?: Timestamp, scheduled?: Timestamp, description?: string
) {
  const time = getCurrentTime();
  const laoId = getCurrentLao().params.message.data.id;

  if (start == undefined && scheduled == undefined)
    throw new Error('RollCall creation failed : neither "start" or "scheduled" field was given');

  if (start != undefined && scheduled != undefined)
    throw new Error('RollCall creation failed : both "start" and "scheduled" fields were given');

  let message = Message.fromData({
    object: ObjectType.ROLL_CALL,
    action: ActionType.CREATE,
    id: Hash.fromString(eventTags.ROLL_CALL, toString64(laoId), time.toString(), name), // FIXME how is the lao id stored?
    name: name,
    creation: time,
    location: location,
    start: start,
    scheduled: scheduled,
    description: description,
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(laoId), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.ROLL_CALL, ActionType.CREATE);
}

/** handles both re/opening roll calls requests for code reuse */
function _requestRollCall(isReopening: boolean, rollCallId: Number, start?: Timestamp) {
  // FIXME: functionality is clearly incomplete here
  const rollCall = { creation: 444, name: 'r-cName' };
  const laoId = getCurrentLao().params.message.data.id;
  const startTime = (start === undefined) ? getCurrentTime() : start;
  const action = isReopening ? ActionType.REOPEN : ActionType.OPEN;

  let message = Message.fromData({
    object: ObjectType.ROLL_CALL,
    action: action,
    id: Hash.fromString(eventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name),
    start: startTime,
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(laoId), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.ROLL_CALL, action);
}

/** Send a server query asking for the opening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead */
export function requestOpenRollCall(rollCallId: Number, start?: Timestamp) {
  _requestRollCall(false, rollCallId, start);
}

/** Send a server query asking for the reopening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead */
export function requestReopenRollCall(rollCallId: Number, start?: Timestamp) {
  _requestRollCall(true, rollCallId, start);
}

/** Send a server query asking for the closing of a roll call given its id (Number) and the
 * list of attendees (Array of public keys) */
export function requestCloseRollCall(rollCallId: Number, attendees: PublicKey[]) {
  // FIXME: functionality is clearly incomplete here
  const rollCall = { creation: 444, start: 555, name: 'r-cName' }; // TODO get roll call by id from localStorage
  const laoId = getCurrentLao().params.message.data.id;

  let message = Message.fromData({
    object: ObjectType.ROLL_CALL,
    action: ActionType.CLOSE,
    id: Hash.fromString(eventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name),
    start: rollCall.start,
    end: getCurrentTime(),
    attendees: attendees,
  });

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish(_generateSubchannel(laoId), message),
  });

  WebsocketLink.sendRequestToServer(request, ObjectType.ROLL_CALL, ActionType.CLOSE);
}
