import { LaoEvent, LaoEventState, LaoEventType } from 'model/objects/LaoEvent';

// Plain-old-data
export interface RollCallState extends LaoEventState {
  // to be continued
}

export class RollCall implements LaoEvent {
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
      eventType: LaoEventType.ROLL_CALL,
    };
  }
}
