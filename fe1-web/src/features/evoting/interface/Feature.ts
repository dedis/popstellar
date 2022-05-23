import { Hash, PublicKey } from 'core/objects';

export namespace EvotingFeature {
  export interface Lao {
    id: Hash;
    organizer: PublicKey;
    witnesses: PublicKey[];
  }

  export interface EventState {
    eventType: string;
    id: string;
    idAlias?: string;

    start: number;
    end?: number;
  }
}
