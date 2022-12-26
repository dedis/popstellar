import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationScreen, NavigationDrawerScreen } from 'core/navigation/typing/Screen';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Hash, PopToken } from 'core/objects';

export namespace WalletFeature {
  export interface Lao {
    id: Hash;
    name: string;

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

  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }

  export interface WalletScreen extends NavigationScreen {
    id: keyof WalletParamList;
  }
}
