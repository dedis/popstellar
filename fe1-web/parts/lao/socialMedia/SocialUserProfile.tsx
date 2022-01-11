import * as React from 'react';
import {
  FlatList,
  ListRenderItemInfo,
  StyleSheet,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';
import STRINGS from 'res/strings';
import { makeChirpsListOfUser } from 'store';
import { useSelector } from 'react-redux';
import TextBlock from 'components/TextBlock';
import ChirpCard from 'components/ChirpCard';
import { Chirp, ChirpState } from 'model/objects/Chirp';
import ProfileIcon from 'components/ProfileIcon';
import { PublicKey } from 'model/objects';

/**
 * UI for the profile of a user.
 */

const styles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
  } as ViewStyle,
  topView: {
    flexDirection: 'row',
  } as ViewStyle,
  homeTextView: {
    alignSelf: 'flex-start',
    marginTop: 20,
  } as ViewStyle,
  userFeed: {
    flexDirection: 'column',
    marginTop: 20,
  } as ViewStyle,
  textInput: {
    padding: 10,
    borderWidth: 1,
    width: 500,
    alignContent: 'flex-end',
  } as TextStyle,
});

const SocialUserProfile = ({ route }: any) => {
  const userPublicKey = route.params;
  const userChirps = makeChirpsListOfUser(userPublicKey);
  const userChirpList = useSelector(userChirps);

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard
      chirp={Chirp.fromState(item)}
    />
  );

  return (
    <View style={styles.viewCenter}>
      <View style={styles.topView}>
        <ProfileIcon
          publicKey={new PublicKey(userPublicKey)}
          size={8}
          scale={10}
        />
        <View style={styles.homeTextView}>
          <TextBlock text={STRINGS.social_media_navigation_tab_profile} />
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

export type SocialUserParams = {
  SocialUserProfile: { userPublicKey: string }
};
