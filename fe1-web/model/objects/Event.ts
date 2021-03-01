import { Hash, Timestamp } from 'model/objects';

export enum EventType {
  MEETING = 'MEETING',
  ROLL_CALL = 'ROLL_CALL',
  // Election, Poll, etc.
}

export interface EventState {
  eventType: EventType;

  id: string;

  start: number;

  end: number;
}

export interface Event {
  id: Hash;

  start: Timestamp;

  end: Timestamp;

  toState(): EventState;
}
