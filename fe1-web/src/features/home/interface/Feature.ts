import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { NavigationTabScreen } from 'core/navigation/typing/Screen';

export namespace HomeFeature {
  export interface LaoState {
    id: string;
  }

  export interface Lao {
    toState: () => LaoState;
  }

  export interface HomeScreen extends NavigationTabScreen {
    id: keyof HomeParamList;
  }
}
