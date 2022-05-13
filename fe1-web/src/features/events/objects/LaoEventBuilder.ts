import { Election, ElectionState } from 'features/evoting/objects';
import { Meeting, MeetingState } from 'features/meeting/objects';
import { RollCall, RollCallState } from 'features/rollCall/objects';

import { LaoEvent, EventState, LaoEventType } from './Event';

/**
 * Creates the corresponding event given a LaoEventState.
 *
 * @param laoEventState
 */
export function eventFromState(laoEventState: EventState): LaoEvent | undefined {
  switch (laoEventState.eventType) {
    case LaoEventType.MEETING:
      return Meeting.fromState(laoEventState as MeetingState);

    case LaoEventType.ROLL_CALL:
      return RollCall.fromState(laoEventState as RollCallState);

    case LaoEventType.ELECTION:
      return Election.fromState(laoEventState as ElectionState);

    default:
      console.warn(`Can't build unsupported event type: ${laoEventState.eventType}`);
      return undefined;
  }
}
