import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationTabScreen } from 'core/navigation/typing/Screen';
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

  export interface LaoScreen extends NavigationTabScreen {
    id: keyof LaoParamList;
  }

  export interface HomeScreen extends NavigationTabScreen {
    id: keyof HomeParamList;
  }
}
