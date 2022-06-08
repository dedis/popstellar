import { NavigatorScreenParams } from '@react-navigation/core';

import STRINGS from 'resources/strings';

import { HomeParamList } from './HomeParamList';
import { LaoParamList } from './LaoParamList';

export type AppParamList = {
  [STRINGS.navigation_app_home]: NavigatorScreenParams<HomeParamList>;
  [STRINGS.navigation_app_lao]: NavigatorScreenParams<LaoParamList>;
  [STRINGS.navigation_app_wallet_create_seed]: undefined;
  [STRINGS.navigation_app_wallet_insert_seed]: undefined;
  [STRINGS.navigation_app_connect]: undefined;
};
