import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from 'core/navigation/typing/Screen';
import { Hash, PopToken } from 'core/objects';

export namespace SocialFeature {
  export interface Lao {
    id: Hash;
    last_tokenized_roll_call_id?: Hash;
  }

  export interface RollCall {
    id: Hash;

    containsToken(token: PopToken | undefined): boolean;
  }

  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
}
