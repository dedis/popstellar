import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as React from 'react';
import { useContext } from 'react';
import { FlatList, ListRenderItemInfo, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { ProfileIcon, TextBlock } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import { PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import BackButton from '../components/BackButton';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { SocialFeature } from '../interface';
import { Chirp, ChirpState } from '../objects';
import { makeChirpsListOfUser } from '../reducer';
import socialMediaProfileStyles from '../styles/socialMediaProfileStyles';

const styles = StyleSheet.create({
  userInnerView: { marginBottom: 15 } as ViewStyle,
});

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
  const { currentUserPublicKey } = useContext(SocialMediaContext);
  const route = useRoute<NavigationProps['route']>();
  const { userPkString } = route.params;
  const userPublicKey = new PublicKey(userPkString);
  const laoId = SocialHooks.useCurrentLaoId();
  if (!laoId) {
    throw new Error('Impossible to render Social Profile, current lao id is undefined');
  }

  const userChirps = makeChirpsListOfUser(laoId.valueOf())(userPublicKey);
  const userChirpList = useSelector(userChirps);

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard chirp={Chirp.fromState(item)} currentUserPublicKey={currentUserPublicKey} />
  );

  const displayNoUser = () => (
    <View style={socialMediaProfileStyles.viewCenter}>
      <View style={socialMediaProfileStyles.topView}>
        <View style={styles.userInnerView}>
          <BackButton
            navigationTabName={STRINGS.social_media_navigation_tab_search}
            testID="backButtonUserProfile"
          />
        </View>
        <TextBlock text="Impossible to load profile of user: public key not provided." />
      </View>
    </View>
  );

  const displayUser = () => (
    <ScreenWrapper>
      <View style={socialMediaProfileStyles.viewCenter}>
        <View style={socialMediaProfileStyles.topView}>
          <View style={styles.userInnerView}>
            <BackButton
              navigationTabName={STRINGS.social_media_navigation_tab_attendee_list}
              testID="backButtonUserProfile"
            />
          </View>
          <ProfileIcon publicKey={userPublicKey} size={8} scale={10} />
          <View style={socialMediaProfileStyles.textView}>
            <Text style={socialMediaProfileStyles.profileText}>{userPkString}</Text>
            <Text>{`${userChirpList.length} ${
              userChirpList.length === 1 ? 'chirp' : 'chirps'
            }`}</Text>
          </View>
        </View>
        <View style={socialMediaProfileStyles.userFeed}>
          <FlatList
            data={userChirpList}
            renderItem={renderChirpState}
            keyExtractor={(item) => item.id.toString()}
          />
        </View>
      </View>
    </ScreenWrapper>
  );

  return userPublicKey !== undefined ? displayUser() : displayNoUser();
};

export default SocialUserProfile;

export const SocialUserProfileScreen: SocialFeature.SocialSearchScreen = {
  id: STRINGS.social_media_navigation_tab_user_profile,
  Component: SocialUserProfile,
};
