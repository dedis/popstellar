import { Hash, PublicKey } from 'core/objects';

export namespace MeetingFeature {
  export interface Lao {
    id: Hash;

    witnesses: PublicKey[];
  }

  export interface EventState {
    eventType: string;

    id: string;
    idAlias?: string;

    name: string;

    start: number;
    end?: number;
  }

  export interface Event {
    id: Hash;
    name: string;
  }

  /* export enum EventType {
    MEETING = 'MEETING',
  } */
}
