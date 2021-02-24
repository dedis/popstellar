import { Event, EventState, EventType } from './Event';
import { Meeting, MeetingState } from './Meeting';
import { RollCall, RollCallState } from './RollCall';

export function eventFromState(evtState: EventState): Event | undefined {
  switch (evtState.eventType) {
    case EventType.MEETING:
      return Meeting.fromState(evtState as MeetingState);

    case EventType.ROLL_CALL:
      return RollCall.fromState(evtState as RollCallState);

    default:
      console.warn(`Can't build unsupported event type: ${evtState.eventType}`);
      return undefined;
  }
}
