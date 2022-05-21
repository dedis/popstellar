import { Hash, PopToken } from 'core/objects';

export namespace WalletFeature {
  export interface Lao {
    id: Hash;

    // ID of the last roll call for which we have a token
    last_tokenized_roll_call_id?: Hash;
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

  export enum EventType {
    ROLL_CALL = 'ROLL_CALL',
  }

  export enum RollCallStatus {
    CLOSED,
  }

  export interface RollCallState extends EventState {}

  export interface RollCall extends Event {
    toState: () => RollCallState;
    containsToken(token: PopToken | undefined): boolean;
  }
}
