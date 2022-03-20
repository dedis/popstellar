import { Hash, PublicKey } from 'core/objects';

export namespace EvotingFeature {
  export interface Lao {
    id: Hash;
    organizer: PublicKey;
    witnesses: PublicKey[];
  }

  export interface EventState {
    readonly eventType: string;

    readonly id: string;

    readonly idAlias?: string;

    readonly name: string;

    readonly start: number;

    readonly end?: number;
  }

  export interface Event {
    readonly id: Hash;

    toState(): EventState;
  }
}
