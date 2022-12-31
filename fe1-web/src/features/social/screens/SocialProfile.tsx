import * as React from 'react';
import { useContext } from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import Profile from '../components/Profile';
import { SocialMediaContext } from '../context';

/**
 * UI for the profile of the current user.
 */

const SocialProfile = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);

  if (!currentUserPopTokenPublicKey) {
    return (
      <ScreenWrapper>
        <Text style={Typography.base}>{STRINGS.social_media_your_profile_unavailable}</Text>
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
      <Profile publicKey={currentUserPopTokenPublicKey} />
    </ScreenWrapper>
  );
};

export default SocialProfile;
