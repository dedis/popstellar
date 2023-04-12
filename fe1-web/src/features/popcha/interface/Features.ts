import { LaoParamList } from '../../../core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from '../../../core/navigation/typing/Screen';

export namespace PoPchaFeature {
  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
}
