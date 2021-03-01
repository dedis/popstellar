import { Timestamp } from './Timestamp';
import { Hash } from './Hash';
import { LaoEvent, LaoEventState, LaoEventType } from './LaoEvent';

// Plain-old-data
export interface MeetingState extends LaoEventState {
  // to be continued
}

export class Meeting implements LaoEvent {
  public readonly id: Hash;

  public readonly start: Timestamp;

  public readonly end: Timestamp;

  constructor(obj: Partial<Meeting>) {
    this.id = obj.id;
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
      eventType: LaoEventType.MEETING,
    };
  }
}
