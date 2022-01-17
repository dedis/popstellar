import * as React from 'react';
import {
  FlatList,
  ListRenderItemInfo,
  StyleSheet,
  Text,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';
import { makeChirpsListOfUser } from 'store';
import { useSelector } from 'react-redux';
import ChirpCard from 'components/ChirpCard';
import { Chirp, ChirpState } from 'model/objects/Chirp';
import ProfileIcon from 'components/ProfileIcon';
import { PublicKey } from 'model/objects';
import PropTypes from 'prop-types';
import { gray } from 'styles/colors';

/**
 * UI for the profile of a user.
 */
const styles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
  } as ViewStyle,
  topView: {
    marginTop: 20,
    flexDirection: 'column',
    alignSelf: 'flex-start',
  } as ViewStyle,
  textView: {
    alignSelf: 'flex-start',
    marginTop: 15,
  } as ViewStyle,
  profileText: {
    marginBottom: 5,
    fontSize: 22,
    fontWeight: 'bold',
  } as TextStyle,
  userFeed: {
    borderColor: gray,
    borderTopWidth: 1,
    flexDirection: 'column',
    marginTop: 20,
  } as ViewStyle,
});

const SocialUserProfile = (props: IPropTypes) => {
  const { userPublicKey } = props;
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

const propTypes = {
  userPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialUserProfile.prototype = propTypes;

type IPropTypes = {
  userPublicKey: PublicKey,
};

export default SocialUserProfile;
