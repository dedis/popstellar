import { LaoParamList } from '../../../core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from '../../../core/navigation/typing/Screen';

export namespace PoPchaFeature {
  // TODO: add the configuration interface
  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
}
