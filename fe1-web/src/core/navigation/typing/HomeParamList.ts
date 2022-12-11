import { NavigatorScreenParams } from '@react-navigation/core';

import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import STRINGS from 'resources/strings';

export type HomeParamList = {
  [STRINGS.navigation_home_home]: undefined;
  [STRINGS.navigation_home_connect]: NavigatorScreenParams<ConnectParamList>;
};
