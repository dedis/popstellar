import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { NavigationDrawerScreen } from 'core/navigation/typing/Screen';


export namespace LinkedOrganizationsFeature {
  export interface LaoScreen extends NavigationDrawerScreen {
    id: keyof LaoParamList;
  }
}
