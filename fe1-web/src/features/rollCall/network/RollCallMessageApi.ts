import { publish } from 'core/network';
import { channelFromIds, EventTags, Hash, PublicKey, Timestamp } from 'core/objects';

import { CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall } from './messages';

/**
 * Contains all functions to send roll call related messages.
 */

/**
 * Sends a server query asking for the creation of a roll call.
 *
 * @param laoId - The id of the lao to create the roll call in
 * @param name - The name of the roll call
 * @param location - The location of the roll call
 * @param proposedStart - The time at which the attendee scanning will start (pending confirmation)
 * @param proposedEnd - The time at which the attendee scanning will stop (pending confirmation)
 * @param description - Further information regarding the event (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestCreateRollCall(
  laoId: Hash,
  name: string,
  location: string,
  proposedStart: Timestamp,
  proposedEnd: Timestamp,
  description?: string,
): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();

  const message = new CreateRollCall(
    {
      id: Hash.fromArray(EventTags.ROLL_CALL, laoId, time, name),
      name: name,
      creation: time,
      location: location,
      proposed_start: Timestamp.max(time, proposedStart),
      proposed_end: proposedEnd,
      description: description,
    },
    laoId,
  );

  const laoCh = channelFromIds(laoId);
  return publish(laoCh, message);
}

/**
 * Sends a server query asking for the opening of a roll call.
 *
 * @param laoId - The id of the lao to open the roll call in
 * @param rollCallId - The ID of the roll call to open
 * @param start - The time at which the operation happens, defaults to now (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestOpenRollCall(
  laoId: Hash,
  rollCallId: Hash,
  start?: Timestamp,
): Promise<void> {
  const time = start === undefined ? Timestamp.EpochNow() : start;

  const message = new OpenRollCall(
    {
      update_id: OpenRollCall.computeOpenRollCallId(laoId, rollCallId, time),
      opens: rollCallId,
      opened_at: time,
    },
    laoId,
  );

  return publish(channelFromIds(laoId), message);
}

/**
 * Sends a server query asking for the re-opening of a roll call.
 *
 * @param laoId - The id of the lao to reopen the roll call in
 * @param rollCallId - The ID of the roll call to re-open
 * @param start - The time at which the operation happens, defaults to now (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestReopenRollCall(
  laoId: Hash,
  rollCallId: Hash,
  start?: Timestamp,
): Promise<void> {
  const time = start === undefined ? Timestamp.EpochNow() : start;

  const message = new ReopenRollCall(
    {
      update_id: ReopenRollCall.computeOpenRollCallId(laoId, rollCallId, time),
      opens: rollCallId,
      opened_at: time,
    },
    laoId,
  );

  return publish(channelFromIds(laoId), message);
}

/**
 * Sends a server query asking for the closing of a roll call.
 *
 * @param laoId - The id of the lao to close the roll call in
 * @param rollCallId - The ID of the roll call to close
 * @param attendees - The list of the attendees' public keys
 * @param close - The time at which the operation happens, defaults to now (optional)
 * @return A Promise resolving when the operation is completed
 */
export function requestCloseRollCall(
  laoId: Hash,
  rollCallId: Hash,
  attendees: PublicKey[],
  close?: Timestamp,
): Promise<void> {
  const time = close === undefined ? Timestamp.EpochNow() : close;

  const message = new CloseRollCall(
    {
      update_id: CloseRollCall.computeCloseRollCallId(laoId, rollCallId, time),
      closes: rollCallId,
      closed_at: time,
      attendees: attendees,
    },
    laoId,
  );

  return publish(channelFromIds(laoId), message);
}
