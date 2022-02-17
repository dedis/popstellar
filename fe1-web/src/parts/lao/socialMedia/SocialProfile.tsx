import * as React from 'react';
import PropTypes from 'prop-types';
import { Chirp, ChirpState, PublicKey } from 'model/objects';
import TextBlock from 'components/TextBlock';
import ChirpCard from 'components/ChirpCard';
import ProfileIcon from 'components/ProfileIcon';
import { FlatList, ListRenderItemInfo, Text, View } from 'react-native';
import STRINGS from 'res/strings';
import { makeChirpsListOfUser } from 'store';
import { useSelector } from 'react-redux';
import socialMediaProfileStyles from 'styles/stylesheets/socialMediaProfileStyles';

/**
 * UI for the profile of the current user.
 */

const styles = socialMediaProfileStyles;

const SocialProfile = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  const userChirps = makeChirpsListOfUser(currentUserPublicKey);
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

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialProfile.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey;
};

export default SocialProfile;
