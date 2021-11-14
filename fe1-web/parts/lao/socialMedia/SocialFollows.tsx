import * as React from 'react';
import { Text, View } from 'react-native';
import STRINGS from 'res/strings';

const SocialFollows = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
    <Text>{STRINGS.social_media_navigation_tab_follows}</Text>
  </View>
);

export default SocialFollows;
