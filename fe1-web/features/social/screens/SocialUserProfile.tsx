import * as React from 'react';
import {
  FlatList, ListRenderItemInfo, Text, View,
} from 'react-native';
import { useSelector } from 'react-redux';

import ProfileIcon from 'components/ProfileIcon';
import TextBlock from 'components/TextBlock';
import BackButton from 'components/BackButton';
import STRINGS from 'res/strings';

import { ChirpCard } from '../components';
import socialMediaProfileStyles from '../styles/socialMediaProfileStyles';
import { Chirp, ChirpState } from '../objects';
import { makeChirpsListOfUser } from '../reducer';

/**
 * UI for the profile of a user.
 */

const styles = socialMediaProfileStyles;

const SocialUserProfile = ({ route }: any) => {
  const { currentUserPublicKey, userPublicKey } = route.params;
  if (!userPublicKey) {
    return (
      <View style={styles.viewCenter}>
        <View style={styles.topView}>
          <View style={{ marginBottom: 15 }}>
            <BackButton
              navigationTabName={STRINGS.social_media_navigation_tab_search}
              testID="backButtonUserProfile"
            />
          </View>
          <TextBlock text="Impossible to load profile of user: public key not provided." />
        </View>
      </View>
    );
  }

  const userChirps = makeChirpsListOfUser(userPublicKey);
  const userChirpList = useSelector(userChirps);

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard
      chirp={Chirp.fromState(item)}
      currentUserPublicKey={currentUserPublicKey}
    />
  );

  return (
    <View style={styles.viewCenter}>
      <View style={styles.topView}>
        <View style={{ marginBottom: 15 }}>
          <BackButton
            navigationTabName={STRINGS.social_media_navigation_tab_attendee_list}
            testID="backButtonUserProfile"
          />
        </View>
        <ProfileIcon
          publicKey={userPublicKey}
          size={8}
          scale={10}
        />
        <View style={styles.textView}>
          <Text style={styles.profileText}>{userPublicKey.valueOf()}</Text>
          <Text>{`${userChirpList.length} ${userChirpList.length === 1 ? 'chirp' : 'chirps'}`}</Text>
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

export default SocialUserProfile;
