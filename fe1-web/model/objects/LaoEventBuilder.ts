import { Meeting, MeetingState } from 'features/meeting/objects/Meeting';
import { Election, ElectionState } from 'features/evoting/objects/Election';
import { LaoEvent, LaoEventState, LaoEventType } from './LaoEvent';
import { RollCall, RollCallState } from './RollCall';

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
