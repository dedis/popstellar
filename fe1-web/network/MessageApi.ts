import {
  Hash, Lao, PublicKey, Timestamp,
} from 'model/objects';
import {
  CreateLao,
  CreateMeeting,
  CreateRollCall,
  StateLao,
  UpdateLao,
  WitnessMessage,
} from 'model/network/method/message/data';
import { Channel, channelFromId, ROOT_CHANNEL } from 'model/objects/Channel';
import { OpenedLaoStore, KeyPairStore } from 'store';
import { eventTags, getCurrentTime } from './WebsocketUtils';
import { publish } from './WebsocketApi';

/** Send a server query asking for the creation of a LAO with a given name (String) */
export function requestCreateLao(name: string) {
  const time = getCurrentTime();

  const message = new CreateLao({
    id: Hash.fromStringArray(KeyPairStore.getPublicKey().toString(), time.toString(), name),
    name,
    creation: time,
    organizer: KeyPairStore.getPublicKey(),
    witnesses: [],
  });

  publish(ROOT_CHANNEL, message);
}

/** Send a server query asking for a LAO update providing a new name (String) */
export function requestUpdateLao(name: string, witnesses?: PublicKey[]) {
  const time: Timestamp = getCurrentTime();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new UpdateLao({
    id: Hash.fromStringArray(currentLao.organizer.toString(), currentLao.creation.toString(), name),
    name,
    last_modified: time,
    witnesses: (witnesses === undefined) ? currentLao.witnesses : witnesses,
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the current state of a LAO */
export function requestStateLao() {
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new StateLao({
    id: Hash.fromStringArray(
      currentLao.organizer.toString(), currentLao.creation.toString(), currentLao.name,
    ),
    name: currentLao.name,
    creation: currentLao.creation,
    last_modified: getCurrentTime(),
    organizer: currentLao.organizer,
    witnesses: currentLao.witnesses,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage
    modification_signatures: [], // FIXME need modification_signatures from storage
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
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new CreateMeeting({
    id: Hash.fromStringArray(
      eventTags.MEETING, currentLao.id.toString(), currentLao.creation.toString(), name,
    ),
    name,
    start: startTime,
    creation: time,
    location,
    end: endTime,
    extra,
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the state of a meeting *
export function requestStateMeeting(startTime: Timestamp) {
  const laoData = get().params.message.data;

  let message = new StateMeeting({
    id: Hash.fromStringArray(
      eventTags.MEETING, laoData.id.toString(), laoData.creation.toString(), laoData.name
    ),
    name: laoData.name,
    creation: laoData.creation,
    last_modified: getCurrentTime(),
    start: startTime,
    location: laoData.location,
    end: laoData.end,
    extra: laoData.extra,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage
    modification_signatures :[], // FIXME need modification_signatures from storage
  });

  publish(channelFromId(laoData.id), message);
}

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash) {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  publish(channel, message);
}

/** Send a server query asking for the creation of a roll call with a given name (String) and a
 *  given location (String). An optional start time (Timestamp), scheduled time (Timestamp) or
 *  description (String) can be specified */
export function requestCreateRollCall(
  name: string, location: string, start?: Timestamp, scheduled?: Timestamp, description?: string,
) {
  const time: Timestamp = getCurrentTime();
  const currentLao: Lao = OpenedLaoStore.get();

  if (start === undefined && scheduled === undefined) {
    throw new Error('RollCall creation failed : neither "start" or "scheduled" field was given');
  }

  if (start !== undefined && scheduled !== undefined) {
    throw new Error('RollCall creation failed : both "start" and "scheduled" fields were given');
  }

  const message = new CreateRollCall({
    id: Hash.fromStringArray(
      eventTags.ROLL_CALL, currentLao.id.toString(), currentLao.creation.toString(), name,
    ),
    name,
    creation: time,
    location,
    start,
    scheduled,
    roll_call_description: description,
  });

  publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the opening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead *
export function requestOpenRollCall(rollCallId: Number, start?: Timestamp) {
    const rollCall = { creation: 1609455600, name: 'r-cName' }; // FIXME: hardcoded
    const laoId = get().params.message.data.id;
    const startTime = (start === undefined) ? getCurrentTime() : start;

    let message = new OpenRollCall({
      action: ActionType.OPEN,
      id: Hash.fromStringArray(
        eventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name
      ),
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
  // get roll call by id from localStorage
  const rollCall = { creation: 1609455600, start: 1609455601, name: 'r-cName' };
  const laoId = get().params.message.data.id;

  let message = new CloseRollCall({
    action: ActionType.REOPEN,
    id: Hash.fromStringArray(
      eventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name
    ),
    start: rollCall.start,
    end: getCurrentTime(),
    attendees: attendees,
  });

  publish(channelFromId(laoId), message);
}
 */
