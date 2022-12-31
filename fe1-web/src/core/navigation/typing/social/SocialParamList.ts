import { NavigatorScreenParams } from '@react-navigation/core';

import STRINGS from 'resources/strings';

import { SocialHomeParamList } from './SocialHomeParamList';
import { SocialProfileParamList } from './SocialProfileParamList';
import { SocialSearchParamList } from './SocialSearchParamList';
import { SocialTopChirpsParamList } from './SocialTopChirpsParamList';

export type SocialParamList = {
  [STRINGS.social_media_navigation_tab_home]: NavigatorScreenParams<SocialHomeParamList>;
  [STRINGS.social_media_navigation_tab_search]: NavigatorScreenParams<SocialSearchParamList>;
  [STRINGS.social_media_navigation_tab_top_chirps]: NavigatorScreenParams<SocialTopChirpsParamList>;
  [STRINGS.social_media_navigation_tab_profile]: NavigatorScreenParams<SocialProfileParamList>;
};
