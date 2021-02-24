export enum EventType {
  MEETING = 'MEETING',
  ROLL_CALL = 'ROLL_CALL',
  // Election, Poll, etc.
}

export interface EventState {
  eventType: EventType;

  // To be continued
}

export interface Event {
  toState(): EventState;

  // To be continued
}
