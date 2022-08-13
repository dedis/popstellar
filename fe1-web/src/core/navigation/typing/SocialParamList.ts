import { NavigatorScreenParams } from '@react-navigation/core';

import { PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { SocialSearchParamList } from './SocialSearchParamList';

export type SocialParamList = {
  [STRINGS.social_media_navigation_tab_home]: {
    currentUserPublicKey: PublicKey;
  };
  [STRINGS.social_media_navigation_tab_search]: NavigatorScreenParams<SocialSearchParamList>;
  [STRINGS.social_media_navigation_tab_follows]: {
    currentUserPublicKey: PublicKey;
  };
  [STRINGS.social_media_navigation_tab_profile]: {
    currentUserPublicKey: PublicKey;
  };
};
