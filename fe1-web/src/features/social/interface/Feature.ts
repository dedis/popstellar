import { NavigationScreen } from 'core/navigation/typing/Screen';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
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

  export interface SocialScreen extends NavigationScreen {
    id: keyof SocialParamList;
  }

  export interface SocialSearchScreen extends NavigationScreen {
    id: keyof SocialSearchParamList;
  }
}
