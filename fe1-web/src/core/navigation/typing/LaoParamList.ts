import { NavigatorScreenParams } from '@react-navigation/core';

import STRINGS from 'resources/strings';

import { LaoEventsParamList } from './LaoEventsParamList';
import { NotificationParamList } from './NotificationParamList';
import { SocialParamList } from './SocialParamList';
import { WalletParamList } from './WalletParamList';

export type LaoParamList = {
  [STRINGS.navigation_lao_home]: undefined;
  [STRINGS.navigation_lao_notifications]: NavigatorScreenParams<NotificationParamList>;
  [STRINGS.navigation_lao_events]: NavigatorScreenParams<LaoEventsParamList>;
  [STRINGS.navigation_social_media]: NavigatorScreenParams<SocialParamList>;
  [STRINGS.navigation_lao_wallet]: NavigatorScreenParams<WalletParamList>;
};
