import { Hash, Timestamp } from 'model/objects';

export enum LaoEventType {
  MEETING = 'MEETING',
  ROLL_CALL = 'ROLL_CALL',
  // Election, Poll, etc.
}

export interface LaoEventState {
  eventType: LaoEventType;

  id: string;

  start: number;

  end: number;
}

export interface LaoEvent {
  id: Hash;

  start: Timestamp;

  end: Timestamp;

  toState(): LaoEventState;
}
