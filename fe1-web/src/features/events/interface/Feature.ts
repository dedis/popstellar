import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { NavigationScreen } from 'core/navigation/typing/Screen';
import { Hash } from 'core/objects';

export namespace EventFeature {
  export interface Lao {
    id: Hash;
  }

  export interface LaoEventScreen extends NavigationScreen {
    id: keyof LaoEventsParamList;
  }
}
