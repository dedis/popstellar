import {
  EventTags, Hash, Lao, PublicKey, Timestamp,
} from 'model/objects';
import {
  CloseRollCall,
  CreateLao,
  CreateRollCall,
  OpenRollCall,
  ReopenRollCall,
  StateLao,
  UpdateLao,
  WitnessMessage,
} from 'model/network/method/message/data';
import {
  Channel, channelFromIds, ROOT_CHANNEL,
} from 'model/objects/Channel';
import {
  OpenedLaoStore, KeyPairStore,
} from 'store';
import { publish } from './JsonRpcApi';

/**
 * Adapts the starting time if start < creation.
 *
 * @param start
 * @param creation
 */
const adaptStartTime = (creation: Timestamp, start: Timestamp) => ((start.before(creation))
  ? creation : start);

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
    .then(() => channelFromIds(message.id));
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

  return publish(channelFromIds(currentLao.id), message);
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

  return publish(channelFromIds(currentLao.id), message);
}

/** Send a server message to acknowledge witnessing the message message (JS object) */
export function requestWitnessMessage(channel: Channel, messageId: Hash): Promise<void> {
  const message = new WitnessMessage({
    message_id: messageId,
    signature: KeyPairStore.getPrivateKey().sign(messageId),
  });

  return publish(channel, message);
}

/**
 * Send a server query asking for the creation of a roll call
 * @param name the name of the roll call event
 * @param location the location of the roll call event
 * @param proposedStart the time at which the attendee scanning will start (pending confirmation)
 * @param proposedEnd the time at which the attendee scanning will stop (pending confirmation)
 * @param description (optional) further information regarding the event
 * @return A Promise resolving when the operation is completed
 */
export function requestCreateRollCall(
  name: string, location: string,
  proposedStart: Timestamp, proposedEnd: Timestamp,
  description?: string,
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new CreateRollCall({
    id: Hash.fromStringArray(
      EventTags.ROLL_CALL, currentLao.id.toString(), time.toString(), name,
    ),
    name: name,
    creation: time,
    location: location,
    proposed_start: adaptStartTime(time, proposedStart),
    proposed_end: proposedEnd,
    description: description,
  });

  const laoCh = channelFromIds(currentLao.id);
  return publish(laoCh, message);
}

/**
 * Send a server query asking for the opening of a roll call
 * @param rollCallId the ID of the roll call
 * @param start (optional) the time at which the operation happens, defaults to now.
 * @return A Promise resolving when the operation is completed
 */
export function requestOpenRollCall(rollCallId: Hash, start?: Timestamp): Promise<void> {
  const lao: Lao = OpenedLaoStore.get();
  const time = (start === undefined) ? Timestamp.EpochNow() : start;

  const message = new OpenRollCall({
    update_id: Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), rollCallId.toString(), time.toString(),
    ),
    opens: rollCallId,
    opened_at: time,
  });

  return publish(channelFromIds(lao.id), message);
}

/**
 * Send a server query asking for the re-opening of a roll call
 * @param rollCallId the ID of the roll call
 * @param start (optional) the time at which the operation happens, defaults to now.
 * @return A Promise resolving when the operation is completed
 */
export function requestReopenRollCall(rollCallId: Hash, start?: Timestamp): Promise<void> {
  const lao: Lao = OpenedLaoStore.get();
  const time = (start === undefined) ? Timestamp.EpochNow() : start;

  const message = new ReopenRollCall({
    update_id: Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), rollCallId.toString(), time.toString(),
    ),
    opens: rollCallId,
    opened_at: time,
  });

  return publish(channelFromIds(lao.id), message);
}

/**
 * Send a server query asking for the closing of a roll call
 * @param rollCallId the ID of the roll call
 * @param attendees list of the attendees' public keys
 * @param close (optional) the time at which the operation happens, defaults to now.
 * @return A Promise resolving when the operation is completed
 */
export function requestCloseRollCall(
  rollCallId: Hash,
  attendees: PublicKey[],
  close?: Timestamp,
): Promise<void> {
  const lao: Lao = OpenedLaoStore.get();
  const time = (close === undefined) ? Timestamp.EpochNow() : close;

  const message = new CloseRollCall({
    update_id: Hash.fromStringArray(
      EventTags.ROLL_CALL, lao.id.toString(), rollCallId.toString(), time.toString(),
    ),
    closes: rollCallId,
    closed_at: time,
    attendees: attendees,
  });

  return publish(channelFromIds(lao.id), message);
}
