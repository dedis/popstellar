import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationTabScreen } from 'core/navigation/typing/Screen';
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

  export interface LaoScreen extends NavigationTabScreen {
    id: keyof LaoParamList;
  }
}
