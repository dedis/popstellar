import { publish } from 'core/network';
import { channelFromIds, Hash, Timestamp } from 'core/objects';

import { CreateMeeting } from './messages';

/**
 * Contains all functions to send meeting related messages.
 */

/**
 * Sends a server query asking for the creation of a meeting.
 *
 * @param laoId - The id of the lao to create this event in
 * @param name - The name of the meeting
 * @param startTime - The start time of the meeting
 * @param location - The location of the meeting (optional)
 * @param endTime - The end time of the meeting (optional)
 * @param extra - Json object containing extra information about the meeting (optional)
 */
export const requestCreateMeeting = (
  laoId: Hash,
  name: string,
  startTime: Timestamp,
  location: string,
  endTime: Timestamp,
  extra?: {},
): Promise<void> => {
  const time = Timestamp.EpochNow();

  const message = new CreateMeeting(
    {
      id: CreateMeeting.computeMeetingId(laoId, time, name),
      name,
      start: Timestamp.max(time, startTime),
      creation: time,
      location,
      end: endTime,
      extra,
    },
    laoId,
  );

  return publish(channelFromIds(laoId), message);
};
