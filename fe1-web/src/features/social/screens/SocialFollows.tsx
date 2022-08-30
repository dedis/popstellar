import * as React from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import STRINGS from 'resources/strings';

import { SocialFeature } from '../interface';

const SocialFollows = () => (
  <ScreenWrapper>
    <Text>{STRINGS.social_media_navigation_tab_follows}</Text>
  </ScreenWrapper>
);

export default SocialFollows;

export const SocialFollowsScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_follows,
  Component: SocialFollows,
  headerShown: false,
};
