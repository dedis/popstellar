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

  export interface WalletItemGenerator {
    /**
     * The react component that returns a set of list items
     */
    ListItems: React.ComponentType<{ laoId: Hash }>;
    /**
     * This number is here to order the different item groups.
     * In order to be able to insert components in between two existing groups,
     * do *not* use numbers 1,2,3,... but rather ones with big gaps in between,
     * e.g. -9999999999, -1000, -10, 0, 100, ... etc.
     */
    order: number;
  }
}
