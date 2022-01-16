import * as React from 'react';
import {
  FlatList, ListRenderItemInfo, Text, View,
} from 'react-native';
import { makeChirpsListOfUser } from 'store';
import { useSelector } from 'react-redux';
import ChirpCard from 'components/ChirpCard';
import ProfileIcon from 'components/ProfileIcon';
import TextBlock from 'components/TextBlock';
import BackButton from 'components/BackButton';
import { Chirp, ChirpState } from 'model/objects/Chirp';
import socialMediaProfile from 'styles/stylesheets/socialMediaProfile';
import STRINGS from 'res/strings';

/**
 * UI for the profile of a user.
 */

const styles = socialMediaProfile;

const SocialUserProfile = ({ route }: any) => {
  const { userPublicKey } = route.params;
  if (!userPublicKey) {
    return <TextBlock text="Impossible to load profile of user: public key not provided." />;
  }

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
        <View style={{ marginBottom: 15 }}>
          <BackButton navigationTabName={STRINGS.social_media_navigation_tab_search} />
        </View>
        <ProfileIcon
          publicKey={userPublicKey}
          size={8}
          scale={10}
        />
        <View style={styles.textView}>
          <Text style={styles.profileText}>{userPublicKey.valueOf()}</Text>
          <Text>{`${userChirpList.length} chirps`}</Text>
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
