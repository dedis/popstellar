import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as React from 'react';
import { useMemo } from 'react';
import { FlatList, ListRenderItemInfo, Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { ProfileIcon, TextBlock } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialFeature } from '../interface';
import { Chirp, ChirpState } from '../objects';
import { makeChirpsListOfUser } from '../reducer';
import socialMediaProfileStyles from '../styles/socialMediaProfileStyles';

/**
 * UI for the profile of the current user.
 */

const styles = socialMediaProfileStyles;

type NavigationProps = CompositeScreenProps<
  StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_profile>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const SocialProfile = () => {
  const route = useRoute<NavigationProps['route']>();
  const { currentUserPublicKey } = route.params;
  const userChirps = useMemo(
    () => makeChirpsListOfUser(currentUserPublicKey),
    [currentUserPublicKey],
  );
  const userChirpList = useSelector(userChirps);

  if (!currentUserPublicKey || currentUserPublicKey.valueOf() === '') {
    return (
      <View style={styles.textUnavailableView}>
        <TextBlock text={STRINGS.social_media_your_profile_unavailable} />
      </View>
    );
  }

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard chirp={Chirp.fromState(item)} currentUserPublicKey={currentUserPublicKey} />
  );

  return (
    <View style={styles.viewCenter}>
      <View style={styles.topView}>
        <ProfileIcon publicKey={currentUserPublicKey} size={8} scale={10} />
        <View style={styles.textView}>
          <Text style={styles.profileText}>{currentUserPublicKey.valueOf()}</Text>
          <Text>{`${userChirpList.length} ${
            userChirpList.length === 1 ? 'chirp' : 'chirps'
          }`}</Text>
        </View>
      </View>
      <View style={styles.userFeed}>
        <FlatList
          data={userChirpList}
          renderItem={renderChirpState}
          keyExtractor={(item) => item.id.toString()}
        />
      </View>
    </View>
  );
};

export default SocialProfile;

export const SocialProfileScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_profile,
  Component: SocialProfile,
};
