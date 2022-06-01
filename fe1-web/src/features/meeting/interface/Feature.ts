import { Hash, PublicKey } from 'core/objects';

export namespace MeetingFeature {
  export interface Lao {
    id: Hash;

    witnesses: PublicKey[];
  }

  export interface EventState {
    eventType: string;
    id: string;

    start: number;
    end?: number;
  }
}
