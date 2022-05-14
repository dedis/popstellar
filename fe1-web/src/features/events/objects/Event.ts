/**
 * Interface to represent an event within a LAO.
 */

// Serializable Event (using primitive types)
export interface EventState {
  eventType: string;
  id: string;
  idAlias?: string;

  start: number;
  end?: number;
}
