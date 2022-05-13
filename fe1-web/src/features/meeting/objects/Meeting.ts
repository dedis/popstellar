import { Hash, Timestamp } from 'core/objects';

/**
 * Object to represent a meeting.
 */

export interface MeetingState {
  id: string;
  start: number;
  end?: number;
  name: string;
  location: string;
  creation: number;
  lastModified: number;
  extra: object;
}

export class Meeting {
  public static EVENT_TYPE = 'MEETING';

  public readonly id: Hash;

  public readonly name: string;

  public readonly location?: string;

  public readonly creation: Timestamp;

  public readonly lastModified: Timestamp;

  public readonly start: Timestamp;

  public readonly end?: Timestamp;

  public readonly extra: object;

  constructor(obj: Partial<Meeting>) {
    if (obj === undefined || obj === null) {
      throw new Error(
        'Error encountered while creating a Meeting object: undefined/null parameters',
      );
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'Meeting'");
    }
    if (obj.name === undefined) {
      throw new Error("Undefined 'name' when creating 'Meeting'");
    }
    if (obj.creation === undefined) {
      throw new Error("Undefined 'creation' when creating 'Meeting'");
    }
    if (obj.start === undefined) {
      throw new Error("Undefined 'start' when creating 'Meeting'");
    }

    this.id = obj.id;
    this.name = obj.name;
    this.location = obj.location;
    this.creation = obj.creation;
    this.lastModified = obj.lastModified || obj.creation;
    this.start = obj.start;
    this.end = obj.end;
    this.extra = obj.extra || {};
  }

  /**
   * Creates a Meeting object from a MeetingState object.
   *
   * @param meetingState
   */
  public static fromState(meetingState: MeetingState): Meeting {
    return new Meeting({
      id: new Hash(meetingState.id),
      name: meetingState.name,
      location: meetingState.location,
      creation: new Timestamp(meetingState.creation),
      lastModified: new Timestamp(
        meetingState.lastModified ? meetingState.lastModified : meetingState.creation,
      ),
      start: new Timestamp(meetingState.start),
      end: meetingState.end ? new Timestamp(meetingState.end) : undefined,
      extra: meetingState.extra ? { ...meetingState.extra } : {},
    });
  }

  /**
   * Creates a MeetingState from the current Meeting object.
   */
  public toState(): MeetingState {
    return JSON.parse(JSON.stringify(this));
  }
}
