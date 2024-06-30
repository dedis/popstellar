import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from 'core/navigation/typing/Screen';
import { Hash, PopToken, PublicKey } from 'core/objects';

export namespace LinkedOrganizationsFeature {
  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
  export interface Lao {
    id: Hash;
    server_addresses: string[];
    organizer: PublicKey;
    last_tokenized_roll_call_id?: Hash | undefined;
    last_roll_call_id?: Hash | undefined;
  }

  export interface RollCall {
    id: Hash;
    name: string;
    status: number;
    attendees?: PublicKey[];

    containsToken(token: PopToken | undefined): boolean;
  }
}
