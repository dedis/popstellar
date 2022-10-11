import { NavigatorScreenParams } from '@react-navigation/core';

import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import STRINGS from 'resources/strings';

import { WalletParamList } from './WalletParamList';

export type HomeParamList = {
  [STRINGS.navigation_home_home]: undefined;
  [STRINGS.navigation_home_connect]: undefined;
  [STRINGS.navigation_home_mock_connect]: NavigatorScreenParams<ConnectParamList>;
  [STRINGS.navigation_home_wallet]: NavigatorScreenParams<WalletParamList>;
};
