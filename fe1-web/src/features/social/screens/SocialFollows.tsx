import * as React from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

const SocialFollows = () => (
  <ScreenWrapper>
    <Text style={[Typography.base, Typography.important]}>
      {STRINGS.social_media_navigation_tab_follows}
    </Text>
  </ScreenWrapper>
);

export default SocialFollows;
