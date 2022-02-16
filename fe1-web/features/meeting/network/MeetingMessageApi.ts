import {
  channelFromIds, EventTags, Hash, Lao, Timestamp,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { publish } from 'network/JsonRpcApi';

import { CreateMeeting } from './messages';

/**
 * Contains all functions to send meeting related messages.
 */

/**
 * Adapts the starting time if start < creation.
 *
 * @param start - The start time of the event
 * @param creation - The creation time of the event
 */
const adaptStartTime = (creation: Timestamp, start: Timestamp) => ((start.before(creation))
  ? creation : start);

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
  name: string, startTime: Timestamp, location: string, endTime: Timestamp, extra?: {},
): Promise<void> {
  const time = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new CreateMeeting({
    id: Hash.fromStringArray(
      EventTags.MEETING, currentLao.id.toString(), currentLao.creation.toString(), name,
    ),
    name,
    start: adaptStartTime(time, startTime),
    creation: time,
    location,
    end: endTime,
    extra,
  });

  return publish(channelFromIds(currentLao.id), message);
}
