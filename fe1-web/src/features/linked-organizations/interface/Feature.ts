import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from 'core/navigation/typing/Screen';
import { Hash, PublicKey } from 'core/objects';

export namespace LinkedOrganizationsFeature {
  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
  export interface Lao {
    id: Hash;
    server_addresses: string[];
    organizer: PublicKey;
  }
}
