import { LaoEvent, LaoEventState, LaoEventType } from './LaoEvent';
import { Hash } from './Hash';
import { Timestamp } from './Timestamp';

export interface RollCallState extends LaoEventState {
  name: string;
  location: string;
  description: string;
  creation: number;
  ongoing: boolean;
}

export class RollCall implements LaoEvent {
  public readonly id: Hash;

  public readonly name: string;

  public readonly location: string;

  public readonly description: string;

  public readonly creation: Timestamp;

  public readonly start: Timestamp;

  public readonly end: Timestamp;

  public readonly ongoing: boolean;

  /* Not yet implemented:
   * This object should probably also keep a list of the time periods
   * during which the roll call was open (or re-opened).
   */

  constructor(obj: Partial<RollCall>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a RollCall object: '
      + 'undefined/null parameters');
    }

    if (!obj.id) throw new Error("Undefined 'id' when creating 'RollCall'");
    if (!obj.name) throw new Error("Undefined 'name' when creating 'RollCall'");
    if (!obj.location) throw new Error("Undefined 'location' when creating 'RollCall'");
    if (!obj.description) throw new Error("Undefined 'description' when creating 'RollCall'");
    if (!obj.creation) throw new Error("Undefined 'creation' when creating 'RollCall'");
    if (!obj.start) throw new Error("Undefined 'start' when creating 'RollCall'");
    if (!obj.end) throw new Error("Undefined 'end' when creating 'RollCall'");
    if (!obj.ongoing) throw new Error("Undefined 'ongoing' when creating 'RollCall'");

    this.id = obj.id;
    this.name = obj.name;
    this.location = obj.location;
    this.description = obj.description;
    this.creation = obj.creation;
    this.start = obj.start;
    this.end = obj.end;
    this.ongoing = obj.ongoing;
  }

  public static fromState(rc: RollCallState): RollCall {
    return new RollCall({
      id: new Hash(rc.id),
      name: rc.name,
      location: rc.location,
      description: rc.description,
      creation: new Timestamp(rc.creation),
      start: new Timestamp(rc.start),
      end: new Timestamp(rc.end),
      ongoing: rc.ongoing,
    });
  }

  public toState(): RollCallState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return {
      ...obj,
      eventType: LaoEventType.ROLL_CALL,
    };
  }
}
