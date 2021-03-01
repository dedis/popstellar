import { Timestamp } from 'model/objects/Timestamp';
import { Event, EventState, EventType } from './Event';

// Plain-old-data
export interface MeetingState extends EventState {
  // to be continued
}

export class Meeting implements Event {
  public readonly start: Timestamp;

  public readonly end: Timestamp;

  constructor(obj: Partial<Meeting>) {
    this.start = obj.start;
    this.end = obj.end;
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
