import { channelFromIds, EventTags, Hash, Timestamp } from 'core/objects';
import { Lao } from 'features/lao/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { publish } from 'core/network/jsonrpc/JsonRpcApi';

import { CreateMeeting } from './messages';

/**
 * Contains all functions to send meeting related messages.
 */

/**
 * Sends a server query asking for the creation of a meeting.
 *
 * @param name - The name of the meeting
 * @param startTime - The start time of the meeting
 * @param location - The location of the meeting (optional)
 * @param endTime - The end time of the meeting (optional)
 * @param extra - Json object containing extra information about the meeting (optional)
 */
export function requestCreateMeeting(
  name: string,
  startTime: Timestamp,
  location: string,
  endTime: Timestamp,
  extra?: {},
): Promise<void> {
  const time = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new CreateMeeting({
    id: Hash.fromStringArray(
      EventTags.MEETING,
      currentLao.id.toString(),
      currentLao.creation.toString(),
      name,
    ),
    name,
    start: Timestamp.max(time, startTime),
    creation: time,
    location,
    end: endTime,
    extra,
  });

  return publish(channelFromIds(currentLao.id), message);
}
