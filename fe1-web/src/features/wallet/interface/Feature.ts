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
  }

  export enum EventType {
    ROLL_CALL = 'ROLL_CALL',
  }

  export enum RollCallStatus {
    CLOSED,
  }

  export interface RollCall {
    id: Hash;
    name: string;

    containsToken(token: PopToken | undefined): boolean;
  }
}
