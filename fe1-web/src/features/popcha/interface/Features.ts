import { PoPchaParamList } from '../../../core/navigation/typing/PoPchaParamList';
import { NavigationDrawerScreen, NavigationScreen } from "../../../core/navigation/typing/Screen";
import { LaoParamList } from "../../../core/navigation/typing/LaoParamList";

export namespace PoPchaFeature {
  // TODO: add the configuration interface
  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
}
