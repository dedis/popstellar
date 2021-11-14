import * as React from 'react';
import { Text, View } from 'react-native';
import STRINGS from 'res/strings';

const SocialProfile = () => (
  <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
    <Text>{STRINGS.social_media_navigation_tab_profile}</Text>
  </View>
);

export default SocialProfile;
