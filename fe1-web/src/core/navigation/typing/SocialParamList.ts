import { NavigatorScreenParams } from '@react-navigation/core';

import STRINGS from 'resources/strings';

import { SocialSearchParamList } from './SocialSearchParamList';

export type SocialParamList = {
  [STRINGS.social_media_navigation_tab_home]: {
    currentUserPublicKey: string;
  };
  [STRINGS.social_media_navigation_tab_search]: NavigatorScreenParams<SocialSearchParamList>;
  [STRINGS.social_media_navigation_tab_follows]: {
    currentUserPublicKey: string;
  };
  [STRINGS.social_media_navigation_tab_profile]: {
    currentUserPublicKey: string;
  };
};
