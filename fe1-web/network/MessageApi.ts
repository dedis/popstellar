import {
  EventTags, Hash, Lao, PublicKey, Timestamp,
} from 'model/objects';
import {
  CreateLao,
  CreateMeeting,
  CreateRollCall,
  StateLao,
  UpdateLao,
  WitnessMessage,
} from 'model/network/method/message/data';
import {
  Channel, channelFromId, ROOT_CHANNEL,
} from 'model/objects/Channel';
import {
  OpenedLaoStore, KeyPairStore,
} from 'store';
import { publish } from './JsonRpcApi';

/** Send a server query asking for the creation of a LAO with a given name (String) */
export function requestCreateLao(laoName: string): Promise<Channel> {
  const time = Timestamp.EpochNow();
  const pubKey = KeyPairStore.getPublicKey();

  const message = new CreateLao({
    id: Hash.fromStringArray(pubKey.toString(), time.toString(), laoName),
    name: laoName,
    creation: time,
    organizer: pubKey,
    witnesses: [],
  });

  return publish(ROOT_CHANNEL, message)
    .then(() => {
      console.info(`LAO was created with ID: ${message.id}`);
      return channelFromId(message.id);
    });
}

/** Send a server query asking for a LAO update providing a new name (String) */
export function requestUpdateLao(name: string, witnesses?: PublicKey[]): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new UpdateLao({
    id: Hash.fromStringArray(currentLao.organizer.toString(), currentLao.creation.toString(), name),
    name,
    last_modified: time,
    witnesses: (witnesses === undefined) ? currentLao.witnesses : witnesses,
  });

  return publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the current state of a LAO */
export function requestStateLao(): Promise<void> {
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new StateLao({
    id: Hash.fromStringArray(
      currentLao.organizer.toString(), currentLao.creation.toString(), currentLao.name,
    ),
    name: currentLao.name,
    creation: currentLao.creation,
    last_modified: Timestamp.EpochNow(),
    organizer: currentLao.organizer,
    witnesses: currentLao.witnesses,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage
    modification_signatures: [], // FIXME need modification_signatures from storage
  });

  return publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the creation of a meeting given a certain name (String),
 *  startTime (Timestamp), optional location (String), optional end time (Timestamp) and optional
 *  extra information (Json object) */
export function requestCreateMeeting(
  name: string, startTime: Timestamp, location?: string, endTime?: Timestamp, extra?: {},
): Promise<void> {
  const time = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new CreateMeeting({
    id: Hash.fromStringArray(
      EventTags.MEETING, currentLao.id.toString(), currentLao.creation.toString(), name,
    ),
    name,
    start: startTime,
    creation: time,
    location,
    end: endTime,
    extra,
  });

  return publish(channelFromId(currentLao.id), message);
}

/** Send a server query asking for the state of a meeting *
export function requestStateMeeting(startTime: Timestamp): Promise<void> {
  const laoData = get().params.message.data;

  let message = new StateMeeting({
    id: Hash.fromStringArray(
      EventTags.MEETING, laoData.id.toString(), laoData.creation.toString(), laoData.name
    ),
    name: laoData.name,
    creation: laoData.creation,
    last_modified: Timestamp.EpochNow(),
    start: startTime,
    location: laoData.location,
    end: laoData.end,
    extra: laoData.extra,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage
    modification_signatures :[], // FIXME need modification_signatures from storage
  });

  return publish(channelFromId(laoData.id), message);
}

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash): Promise<void> {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  return publish(channel, message);
}

/** Send a server query asking for the creation of a roll call with a given name (String) and a
 *  given location (String). An optional start time (Timestamp), scheduled time (Timestamp) or
 *  description (String) can be specified */
export function requestCreateRollCall(
  name: string, location: string, start?: Timestamp, scheduled?: Timestamp, description?: string,
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  if (start === undefined && scheduled === undefined) {
    throw new Error('RollCall creation failed : neither "start" or "scheduled" field was given');
  }

  if (start !== undefined && scheduled !== undefined) {
    throw new Error('RollCall creation failed : both "start" and "scheduled" fields were given');
  }

  const message = new CreateRollCall({
    id: Hash.fromStringArray(
      EventTags.ROLL_CALL, currentLao.id.toString(), currentLao.creation.toString(), name,
    ),
    name: name,
    creation: time,
    location: location,
    start: start,
    scheduled: scheduled,
    roll_call_description: description,
  });

  const laoCh = channelFromId(currentLao.id);
  return publish(laoCh, message);
}

/** Send a server query asking for the opening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead *
export function requestOpenRollCall(rollCallId: Number, start?: Timestamp): Promise<void> {
    const rollCall = { creation: 1609455600, name: 'r-cName' }; // FIXME: hardcoded
    const laoId = get().params.message.data.id;
    const startTime = (start === undefined) ? Timestamp.EpochNow() : start;

    let message = new OpenRollCall({
      action: ActionType.OPEN,
      id: Hash.fromStringArray(
        EventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name
      ),
      start: startTime,
    });

    return publish(channelFromId(laoId), message);
}

/** Send a server query asking for the reopening of a roll call given its id (Number) and an
 * optional start time (Timestamp). If the start time is not specified, then the current time
 * will be used instead *
export function requestReopenRollCall(rollCallId: Number, start?: Timestamp): Promise<void> {
    // FIXME: not implemented
}

/** Send a server query asking for the closing of a roll call given its id (Number) and the
 * list of attendees (Array of public keys) *
export function requestCloseRollCall(rollCallId: Number, attendees: PublicKey[]): Promise<void> {
  // FIXME: functionality is clearly incomplete here
  // get roll call by id from localStorage
  const rollCall = { creation: 1609455600, start: 1609455601, name: 'r-cName' };
  const laoId = get().params.message.data.id;

  let message = new CloseRollCall({
    action: ActionType.REOPEN,
    id: Hash.fromStringArray(
      EventTags.ROLL_CALL, toString64(laoId), rollCall.creation.toString(), rollCall.name
    ),
    start: rollCall.start,
    end: Timestamp.EpochNow(),
    attendees: attendees,
  });

  return publish(channelFromId(laoId), message);
}
 */
