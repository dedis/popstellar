import * as React from 'react';
import { Text, View } from 'react-native';

import STRINGS from 'resources/strings';

import { SocialFeature } from '../interface';

const SocialFollows = () => (
  <View>
    <Text>{STRINGS.social_media_navigation_tab_follows}</Text>
  </View>
);

export default SocialFollows;

export const SocialFollowsScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_follows,
  Component: SocialFollows,
  headerShown: false,
};
