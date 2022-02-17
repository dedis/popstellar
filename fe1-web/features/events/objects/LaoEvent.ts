import { Hash, Timestamp } from 'model/objects';

/**
 * Interface to represent an event within a LAO.
 */

// Represents all types of events
export enum LaoEventType {
  MEETING = 'MEETING',
  ROLL_CALL = 'ROLL_CALL',
  ELECTION = 'ELECTION',
}

// Serializable LaoEvent (using primitive types)
export interface LaoEventState {
  readonly eventType: LaoEventType;

  readonly id: string;

  readonly idAlias?: string;

  readonly start: number;

  readonly end?: number;
}

export interface LaoEvent {
  readonly id: Hash;

  readonly idAlias?: Hash;

  readonly start: Timestamp;

  readonly end?: Timestamp;

  toState(): LaoEventState;
}
