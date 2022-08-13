import { NavigatorScreenParams } from '@react-navigation/core';

import STRINGS from 'resources/strings';

import { SocialSearchParamList } from './SocialSearchParamList';

export type SocialParamList = {
  [STRINGS.social_media_navigation_tab_home]: undefined;
  [STRINGS.social_media_navigation_tab_search]: NavigatorScreenParams<SocialSearchParamList>;
  [STRINGS.social_media_navigation_tab_follows]: undefined;
  [STRINGS.social_media_navigation_tab_profile]: undefined;
};
