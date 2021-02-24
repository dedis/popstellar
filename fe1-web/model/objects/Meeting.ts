import { Event, EventState, EventType } from './Event';

// Plain-old-data
export interface MeetingState extends EventState {
  // to be continued
}

export class Meeting implements Event {
  // to be continued

  constructor(obj: Partial<Meeting>) {
    // to be continued
  }

  public static fromState(rc: MeetingState): Meeting {
    const { eventType, ...rest } = rc;
    return new Meeting(rest);
  }

  public toState(): MeetingState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return {
      ...obj,
      eventType: EventType.MEETING,
    };
  }
}
