import * as React from 'react';
import { useContext, useMemo } from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { ProfileIcon } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
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
    () => makeChirpsListOfUser(laoId)(currentUserPopTokenPublicKey),
    [currentUserPopTokenPublicKey, laoId],
  );
  const userChirpList = useSelector(userChirps);

  if (!currentUserPopTokenPublicKey) {
    return (
      <View style={styles.textUnavailableView}>
        <Text style={Typography.base}>{STRINGS.social_media_your_profile_unavailable}</Text>
      </View>
    );
  }

  return (
    <ScreenWrapper>
      <View>
        <ProfileIcon publicKey={currentUserPopTokenPublicKey} size={8} scale={10} />
        <View style={styles.textView}>
          <Text style={[Typography.base, Typography.important]} numberOfLines={1}>
            {currentUserPopTokenPublicKey.valueOf()}
          </Text>
          <Text>{`${userChirpList.length} ${
            userChirpList.length === 1 ? 'chirp' : 'chirps'
          }`}</Text>
        </View>
      </View>
      <View style={styles.userFeed}>
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
    </ScreenWrapper>
  );
};

export default SocialProfile;
