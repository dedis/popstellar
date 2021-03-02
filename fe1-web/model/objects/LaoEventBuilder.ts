import { LaoEvent, LaoEventState, LaoEventType } from './LaoEvent';
import { Meeting, MeetingState } from './Meeting';
import { RollCall, RollCallState } from './RollCall';

export function eventFromState(evtState: LaoEventState): LaoEvent | undefined {
  switch (evtState.eventType) {
    case LaoEventType.MEETING:
      return Meeting.fromState(evtState as MeetingState);

    case LaoEventType.ROLL_CALL:
      return RollCall.fromState(evtState as RollCallState);

    default:
      console.warn(`Can't build unsupported event type: ${evtState.eventType}`);
      return undefined;
  }
}
