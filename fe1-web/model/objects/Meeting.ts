import { LaoEvent, LaoEventState, LaoEventType } from './LaoEvent';
import { Timestamp } from './Timestamp';
import { Hash } from './Hash';

export interface MeetingState extends LaoEventState {
  name: string;
  location: string;
  creation: number;
  extra: object;
}

export class Meeting implements LaoEvent {
  public readonly id: Hash;

  public readonly name: string;

  public readonly location: string;

  public readonly creation: Timestamp;

  public readonly start: Timestamp;

  public readonly end: Timestamp;

  public readonly extra: object;

  constructor(obj: Partial<Meeting>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a Meeting object: '
        + 'undefined/null parameters');
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'Meeting'");
    }
    if (obj.name === undefined) {
      throw new Error("Undefined 'name' when creating 'Meeting'");
    }
    if (obj.location === undefined) {
      throw new Error("Undefined 'location' when creating 'Meeting'");
    }
    if (obj.creation === undefined) {
      throw new Error("Undefined 'creation' when creating 'Meeting'");
    }
    if (obj.start === undefined) {
      throw new Error("Undefined 'start' when creating 'Meeting'");
    }
    if (obj.end === undefined) {
      throw new Error("Undefined 'end' when creating 'Meeting'");
    }

    this.id = obj.id;
    this.name = obj.name;
    this.location = obj.location;
    this.creation = obj.creation;
    this.start = obj.start;
    this.end = obj.end;
    this.extra = obj.extra || {};
  }

  public static fromState(m: MeetingState): Meeting {
    return new Meeting({
      id: new Hash(m.id),
      name: m.name,
      location: m.location,
      creation: new Timestamp(m.creation),
      start: new Timestamp(m.start),
      end: new Timestamp(m.end),
      extra: { ...m.extra },
    });
  }

  public toState(): MeetingState {
    const obj: any = JSON.parse(JSON.stringify(this));
    return {
      ...obj,
      eventType: LaoEventType.MEETING,
    };
  }
}
