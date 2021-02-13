import { Hash, Lao, PublicKey, Timestamp } from "Model/Objects";
import { JsonRpcMethod, JsonRpcRequest } from 'Model/Network';
import { Publish } from 'Model/Network/Method';
import { Message } from 'Model/Network/Method/Message';
import {
  CreateLao,
  CreateMeeting,
  CreateRollCall,
  MessageData,
  StateLao,
  UpdateLao,
  WitnessMessage,
} from 'Model/Network/Method/Message/data';
import { eventTags, getCurrentTime } from './WebsocketUtils';
import WebsocketLink from './WebsocketLink';
import { Channel, channelFromId, ROOT_CHANNEL } from "Model/Objects/Channel";
import { getStorageKeyPair, getStorageCurrentLao } from "../Store/Storage";


/** Send a server query asking for the creation of a LAO with a given name (String) */
export function requestCreateLao(name: string) {
  const time = getCurrentTime();

  let message = new CreateLao({
    id: Hash.fromStringArray(getStorageKeyPair().getPublicKey().toString(), time.toString(), name),
    name: name,
    creation: time,
    organizer: getStorageKeyPair().getPublicKey(),
    witnesses: [],
  });

  publish(ROOT_CHANNEL, message);
}

/** Send a server query asking for a LAO update providing a new name (String) */
export function requestUpdateLao(name: string, witnesses?: PublicKey[]) {
  const time: Timestamp = getCurrentTime();
  const currentLao: Lao = getStorageCurrentLao().getCurrentLao();

  let message = new UpdateLao({
    id: Hash.fromStringArray(currentLao.organizer.toString(), currentLao.creation.toString(), name),
    name: name,
    last_modified: time,
    witnesses: (witnesses === undefined) ? currentLao.witnesses : witnesses,
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the current state of a LAO */
export function requestStateLao() {
  const currentLao: Lao = getStorageCurrentLao().getCurrentLao();

  let message = new StateLao({
    id: Hash.fromStringArray(currentLao.organizer.toString(), currentLao.creation.toString(), currentLao.name),
    name: currentLao.name,
    creation: currentLao.creation,
    last_modified: getCurrentTime(),
    organizer: currentLao.organizer,
    witnesses: currentLao.witnesses,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage (waiting for storage)
    modification_signatures :[], // FIXME need modification_signatures from storage (waiting for storage)
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the creation of a meeting given a certain name (String),
 *  startTime (Timestamp), optional location (String), optional end time (Timestamp) and optional
 *  extra information (Json object) */
export function requestCreateMeeting(
  name: string, startTime: Timestamp, location?: string, endTime?: Timestamp, extra?: {},
) {
  const time = getCurrentTime();
  const currentLao: Lao = getStorageCurrentLao().getCurrentLao();

  let message = new CreateMeeting({
    id: Hash.fromStringArray(eventTags.MEETING, currentLao.id.toString(), time.toString(), name),
    name: name,
    start: startTime,
    creation: time,
    location: location,
    end: endTime,
    extra: extra,
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the state of a meeting *
export function requestStateMeeting(startTime: Timestamp) {
  const laoData = getCurrentLao().params.message.data;

  let message = new StateMeeting({
    id: Hash.fromStringArray(eventTags.MEETING, laoData.id.toString(), laoData.creation.toString(), laoData.name),
    name: laoData.name,
    creation: laoData.creation,
    last_modified: getCurrentTime(),
    start: startTime,
    location: laoData.location,
    end: laoData.end,
    extra: laoData.extra,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage (waiting for storage)
    modification_signatures :[], // FIXME need modification_signatures from storage (waiting for storage)
  });

  publish(channelFromId(laoData.id), message);
}

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash) {

  let message = new WitnessMessage({
    message_id: messageId,
    signature: getStorageKeyPair().getPrivateKey().sign(messageId),
  });

  publish(channel, message);
}

/** Send a server query asking for the creation of a roll call with a given name (String) and a
 *  given location (String). An optional start time (Timestamp), scheduled time (Timestamp) or
 *  description (String) can be specified */
export function requestCreateRollCall(
    name: string, location: string, start?: Timestamp, scheduled?: Timestamp, description?: string
) {
  const time: Timestamp = getCurrentTime();
  const currentLao: Lao = getStorageCurrentLao().getCurrentLao();

  if (start === undefined && scheduled === undefined)
    throw new Error('RollCall creation failed : neither "start" or "scheduled" field was given');

  if (start !== undefined && scheduled !== undefined)
    throw new Error('RollCall creation failed : both "start" and "scheduled" fields were given');

  let message = new CreateRollCall({
    id: Hash.fromStringArray(eventTags.ROLL_CALL, currentLao.id.toString(), time.toString(), name),
    name: name,
    creation: time,
    location: location,
    start: start,
    scheduled: scheduled,
    roll_call_description: description,
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the opening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead *
export function requestOpenRollCall(rollCallId: Number, start?: Timestamp) {
    const rollCall = { creation: 1609455600, name: 'r-cName' }; // FIXME: hardcoded
    const laoId = getCurrentLao().params.message.data.id;
    const startTime = (start === undefined) ? getCurrentTime() : start;

    let message = new OpenRollCall({
      action: ActionType.OPEN,
      id: Hash.fromStringArray(eventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name),
      start: startTime,
    });

    publish(channelFromId(laoId), message);
}

/** Send a server query asking for the reopening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead *
export function requestReopenRollCall(rollCallId: Number, start?: Timestamp) {
    // FIXME: not implemented
}

/** Send a server query asking for the closing of a roll call given its id (Number) and the
 * list of attendees (Array of public keys) *
export function requestCloseRollCall(rollCallId: Number, attendees: PublicKey[]) {
  // FIXME: functionality is clearly incomplete here
  const rollCall = { creation: 1609455600, start: 1609455601, name: 'r-cName' }; // TODO get roll call by id from localStorage
  const laoId = getCurrentLao().params.message.data.id;

  let message = new CloseRollCall({
    action: ActionType.REOPEN,
    id: Hash.fromStringArray(eventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name),
    start: rollCall.start,
    end: getCurrentTime(),
    attendees: attendees,
  });

  publish(channelFromId(laoId), message);
}
 */

function publish(channel: Channel, msgData: MessageData) {
  let message = Message.fromData(msgData);

  let request = new JsonRpcRequest({
    method: JsonRpcMethod.PUBLISH,
    params: new Publish({
        channel: channel,
        message: message
    }),
  });
/* // FIXME uncomment once websocket link is refactored
  WebsocketLink.sendRequestToServer(request,
    message.messageData.object,
    message.messageData.action);*/
}

