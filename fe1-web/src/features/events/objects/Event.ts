/**
 * Interface to represent an event within a LAO.
 */

// Serializable Event (using primitive types)
export interface EventState {
  readonly eventType: string;

  readonly id: string;

  readonly idAlias?: string;
}
