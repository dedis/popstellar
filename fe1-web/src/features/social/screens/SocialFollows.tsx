import * as React from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import STRINGS from 'resources/strings';

const SocialFollows = () => (
  <ScreenWrapper>
    <Text>{STRINGS.social_media_navigation_tab_follows}</Text>
  </ScreenWrapper>
);

export default SocialFollows;
