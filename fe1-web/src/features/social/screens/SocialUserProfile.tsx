import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as React from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { ProfileIcon } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import { PublicKey } from 'core/objects';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialHooks } from '../hooks';
import { makeChirpsListOfUser } from '../reducer';
import socialMediaProfileStyles from '../styles/socialMediaProfileStyles';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<SocialSearchParamList, typeof STRINGS.social_media_navigation_tab_user_profile>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * UI for the profile of a user.
 */
const SocialUserProfile = () => {
  const route = useRoute<NavigationProps['route']>();
  const { userPkString } = route.params;
  const userPublicKey = new PublicKey(userPkString);
  const laoId = SocialHooks.useCurrentLaoId();
  if (!laoId) {
    throw new Error('Impossible to render Social Profile, current lao id is undefined');
  }

  const userChirps = makeChirpsListOfUser(laoId)(userPublicKey);
  const userChirpList = useSelector(userChirps);

  const displayNoUser = () => (
    <View>
      <View>
        <Text style={Typography.base}>
          Impossible to load profile of user: public key not provided.
        </Text>
      </View>
    </View>
  );

  const displayUser = () => (
    <ScreenWrapper>
      <View>
        <View>
          <ProfileIcon publicKey={userPublicKey} size={8} scale={10} />
          <View style={socialMediaProfileStyles.textView}>
            <Text style={[Typography.base, Typography.important]} numberOfLines={1}>
              {userPkString}
            </Text>
            <Text>{`${userChirpList.length} ${
              userChirpList.length === 1 ? 'chirp' : 'chirps'
            }`}</Text>
          </View>
        </View>
        <View style={socialMediaProfileStyles.userFeed}>
          <View style={List.container}>
            {userChirpList.map((chirp, i) => (
              <ChirpCard
                key={chirp.id.toString()}
                chirp={chirp}
                isFirstItem={false /* no round borders at the top */}
                isLastItem={i === userChirpList.length - 1}
              />
            ))}
          </View>
        </View>
      </View>
    </ScreenWrapper>
  );

  return userPublicKey !== undefined ? displayUser() : displayNoUser();
};

export default SocialUserProfile;
