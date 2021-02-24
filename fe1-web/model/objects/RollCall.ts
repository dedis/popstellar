import { Event, EventState, EventType } from './Event';

// Plain-old-data
export interface RollCallState extends EventState {
  // to be continued
}

export class RollCall implements Event {
  // to be continued

  constructor(obj: Partial<RollCall>) {
    // to be continued
  }

  public static fromState(rc: RollCallState): RollCall {
    const { eventType, ...rest } = rc;
    return new RollCall(rest);
  }

  public toState(): RollCallState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return {
      ...obj,
      eventType: EventType.ROLL_CALL,
    };
  }
}
