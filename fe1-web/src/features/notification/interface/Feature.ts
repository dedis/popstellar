import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationTabScreen } from 'core/navigation/typing/Screen';

export namespace NotificationFeature {
  export interface LaoScreen extends NavigationTabScreen {
    id: keyof LaoParamList;
  }
}
