import { Meeting, MeetingState } from 'features/meeting/objects/Meeting';
import { LaoEvent, LaoEventState, LaoEventType } from './LaoEvent';
import { RollCall, RollCallState } from './RollCall';
import { Election, ElectionState } from './Election';

export function eventFromState(evtState: LaoEventState): LaoEvent | undefined {
  switch (evtState.eventType) {
    case LaoEventType.MEETING:
      return Meeting.fromState(evtState as MeetingState);

    case LaoEventType.ROLL_CALL:
      return RollCall.fromState(evtState as RollCallState);

    case LaoEventType.ELECTION:
      return Election.fromState(evtState as ElectionState);

    default:
      console.warn(`Can't build unsupported event type: ${evtState.eventType}`);
      return undefined;
  }
}
