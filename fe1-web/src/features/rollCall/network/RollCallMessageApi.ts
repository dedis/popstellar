import { channelFromIds, EventTags, Hash, PublicKey, Timestamp } from 'model/objects';
import { Lao } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { publish } from 'network/JsonRpcApi';

import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from './messages';

/**
 * Contains all functions to send roll call related messages.
 */

/**
 * Sends a server query asking for the creation of a roll call.
 *
 * @param name - The name of the roll call
 * @param location - The location of the roll call
 * @param proposedStart - The time at which the attendee scanning will start (pending confirmation)
 * @param proposedEnd - The time at which the attendee scanning will stop (pending confirmation)
 * @param description - Further information regarding the event (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestCreateRollCall(
  name: string,
  location: string,
  proposedStart: Timestamp,
  proposedEnd: Timestamp,
  description?: string,
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new CreateRollCall({
    id: Hash.fromStringArray(EventTags.ROLL_CALL, currentLao.id.toString(), time.toString(), name),
    name: name,
    creation: time,
    location: location,
    proposed_start: Timestamp.max(time, proposedStart),
    proposed_end: proposedEnd,
    description: description,
  });

  const laoCh = channelFromIds(currentLao.id);
  return publish(laoCh, message);
}

/**
 * Sends a server query asking for the opening of a roll call.
 *
 * @param rollCallId - The ID of the roll call to open
 * @param start - The time at which the operation happens, defaults to now (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestOpenRollCall(rollCallId: Hash, start?: Timestamp): Promise<void> {
  const lao: Lao = OpenedLaoStore.get();
  const time = start === undefined ? Timestamp.EpochNow() : start;

  const message = new OpenRollCall({
    update_id: Hash.fromStringArray(
      EventTags.ROLL_CALL,
      lao.id.toString(),
      rollCallId.toString(),
      time.toString(),
    ),
    opens: rollCallId,
    opened_at: time,
  });

  return publish(channelFromIds(lao.id), message);
}

/**
 * Sends a server query asking for the re-opening of a roll call.
 *
 * @param rollCallId - The ID of the roll call to re-open
 * @param start - The time at which the operation happens, defaults to now (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestReopenRollCall(rollCallId: Hash, start?: Timestamp): Promise<void> {
  const lao: Lao = OpenedLaoStore.get();
  const time = start === undefined ? Timestamp.EpochNow() : start;

  const message = new ReopenRollCall({
    update_id: Hash.fromStringArray(
      EventTags.ROLL_CALL,
      lao.id.toString(),
      rollCallId.toString(),
      time.toString(),
    ),
    opens: rollCallId,
    opened_at: time,
  });

  return publish(channelFromIds(lao.id), message);
}

/**
 * Sends a server query asking for the closing of a roll call.
 *
 * @param rollCallId - The ID of the roll call to close
 * @param attendees - The list of the attendees' public keys
 * @param close - The time at which the operation happens, defaults to now (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestCloseRollCall(
  rollCallId: Hash,
  attendees: PublicKey[],
  close?: Timestamp,
): Promise<void> {
  const lao: Lao = OpenedLaoStore.get();
  const time = close === undefined ? Timestamp.EpochNow() : close;

  const message = new CloseRollCall({
    update_id: Hash.fromStringArray(
      EventTags.ROLL_CALL,
      lao.id.toString(),
      rollCallId.toString(),
      time.toString(),
    ),
    closes: rollCallId,
    closed_at: time,
    attendees: attendees,
  });

  return publish(channelFromIds(lao.id), message);
}
