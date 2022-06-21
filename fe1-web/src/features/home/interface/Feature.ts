import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { NavigationTabScreen } from 'core/navigation/typing/Screen';
import { Hash, PublicKey, Timestamp } from 'core/objects';

export namespace HomeFeature {
  export interface LaoState {
    name: string;
    id: string;
    creation: number;
    last_modified: number;
    organizer: string;
    witnesses: string[];
    last_roll_call_id?: string;
    last_tokenized_roll_call_id?: string;
    server_addresses: string[];
    subscribed_channels: string[];
  }

  export interface Lao {
    name: string;
    id: Hash;
    creation: Timestamp;
    last_modified: Timestamp;
    organizer: PublicKey;
    witnesses: PublicKey[];
    last_roll_call_id?: Hash;
    last_tokenized_roll_call_id?: Hash;
    server_addresses: string[];
    subscribed_channels: string[];

    toState: () => LaoState;
  }

  export interface HomeScreen extends NavigationTabScreen {
    id: keyof HomeParamList;
  }
}
