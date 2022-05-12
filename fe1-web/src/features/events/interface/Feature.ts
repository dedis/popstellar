import { Hash, PopToken } from 'core/objects';

export namespace EventFeature {
  export interface Lao {
    id: Hash;
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
    id: Hash;
    readonly name: string;

    toState(): EventState;
  }

  export interface RollCallState extends EventState {}

  export interface RollCall extends Event {
    toState: () => RollCallState;
    containsToken(token: PopToken | undefined): boolean;
  }
}
