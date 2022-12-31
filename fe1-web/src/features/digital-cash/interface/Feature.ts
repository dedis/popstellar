import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from 'core/navigation/typing/Screen';
import { Hash, PopToken, PublicKey } from 'core/objects';

export namespace DigitalCashFeature {
  export interface Lao {
    id: Hash;
  }

  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }

  export interface RollCall {
    id: Hash;
    name: string;
    status: number;
    attendees?: PublicKey[];

    containsToken(token: PopToken | undefined): boolean;
  }
}
