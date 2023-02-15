import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as React from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/social';
import { SocialSearchParamList } from 'core/navigation/typing/social/SocialSearchParamList';
import { PublicKey } from 'core/objects';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import Profile from '../components/Profile';
import { SocialHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<
    SocialSearchParamList,
    typeof STRINGS.social_media_search_navigation_user_profile
  >,
  CompositeScreenProps<
    StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_search>,
    CompositeScreenProps<
      StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
      StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
    >
  >
>;

/**
 * UI for the profile of a user.
 */
const SocialUserProfile = () => {
  const route = useRoute<NavigationProps['route']>();
  const { userPkString } = route.params;
  const userPublicKey = React.useMemo(() => new PublicKey(userPkString), [userPkString]);
  const laoId = SocialHooks.useCurrentLaoId();
  if (!laoId) {
    throw new Error('Impossible to render Social Profile, current lao id is undefined');
  }

  if (!userPublicKey) {
    return (
      <ScreenWrapper>
        <Text style={Typography.base}>
          Impossible to load profile of user: public key not provided.
        </Text>
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
      <Profile publicKey={userPublicKey} />
    </ScreenWrapper>
  );
};

export default SocialUserProfile;
