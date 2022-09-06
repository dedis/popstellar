import * as React from 'react';
import { useContext, useMemo } from 'react';
import { FlatList, ListRenderItemInfo, Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { ProfileIcon, TextBlock } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { SocialFeature } from '../interface';
import { Chirp, ChirpState } from '../objects';
import { makeChirpsListOfUser } from '../reducer';
import socialMediaProfileStyles from '../styles/socialMediaProfileStyles';

/**
 * UI for the profile of the current user.
 */

const styles = socialMediaProfileStyles;

const SocialProfile = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);
  const laoId = SocialHooks.useCurrentLaoId();
  if (!laoId) {
    throw new Error('Impossible to render Social Profile, current lao id is undefined');
  }

  const userChirps = useMemo(
    () => makeChirpsListOfUser(laoId.valueOf())(currentUserPopTokenPublicKey),
    [currentUserPopTokenPublicKey, laoId],
  );
  const userChirpList = useSelector(userChirps);

  if (!currentUserPopTokenPublicKey) {
    return (
      <View style={styles.textUnavailableView}>
        <TextBlock text={STRINGS.social_media_your_profile_unavailable} />
      </View>
    );
  }

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard chirp={Chirp.fromState(item)} currentUserPublicKey={currentUserPopTokenPublicKey} />
  );

  return (
    <ScreenWrapper>
      <View style={styles.viewCenter}>
        <View style={styles.topView}>
          <ProfileIcon publicKey={currentUserPopTokenPublicKey} size={8} scale={10} />
          <View style={styles.textView}>
            <Text style={styles.profileText}>{currentUserPopTokenPublicKey.valueOf()}</Text>
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
    </ScreenWrapper>
  );
};

export default SocialProfile;

export const SocialProfileScreen: SocialFeature.SocialScreen = {
  id: STRINGS.social_media_navigation_tab_profile,
  Component: SocialProfile,
};
