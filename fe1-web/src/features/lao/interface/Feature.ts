import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationScreen, NavigationTabScreen } from 'core/navigation/typing/Screen';

export namespace LaoFeature {
  export interface LaoEventScreen extends NavigationScreen {
    id: keyof LaoEventsParamList;
  }

  export interface LaoScreen extends NavigationTabScreen {
    id: keyof LaoParamList;
  }

  export interface LaoConnection {
    server: string;
    lao: string;
  }
}
