import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { NavigationScreen } from 'core/navigation/typing/Screen';
import { Hash, PublicKey } from 'core/objects';

export namespace RollCallFeature {
  export interface Lao {
    id: Hash;

    witnesses: PublicKey[];
  }

  export interface EventState {
    eventType: string;
    id: string;

    start: number;
    end?: number;
  }

  export interface LaoEventScreen extends NavigationScreen {
    id: keyof LaoEventsParamList;
  }
}
