import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  View, Text, StyleSheet, ViewStyle, TextStyle,
} from 'react-native';
import { getUserSocialChannel, Hash, PublicKey } from 'model/objects';
import { gray } from 'styles/colors';
import { Ionicons } from '@expo/vector-icons';
import STRINGS from 'res/strings';
import { subscribeToChannel } from 'network/CommunicationApi';
import WideButtonView from './WideButtonView';

/**
 * Component that shows a user's profile picture, his public key and two buttons:
 * - Follow, which subscribes to the user's social channel.
 * - Profile, that navigates to the user's profile page.
 *
 * @remarks
 * For now, it is not possible to unfollow users, and you have to follow someone to access to
 * his profile.
 */
const styles = StyleSheet.create({
  container: {
    borderColor: gray,
    borderTopWidth: 0,
    borderWidth: 1,
    flexDirection: 'row',
    padding: 10,
    width: 600,
  } as ViewStyle,
  leftView: {
    width: 60,
  } as ViewStyle,
  publicKeyText: {
    fontSize: 18,
    fontWeight: '600',
  } as TextStyle,
  rightView: {
    flexDirection: 'column',
  } as ViewStyle,
  buttonsView: {
    flexDirection: 'row',
  } as ViewStyle,
  buttonView: {
    flex: 1,
  } as ViewStyle,
});

const UserListItem = (props: IPropTypes) => {
  const [isFollowing, setIsFollowing] = useState(false);
  const { laoId, publicKey } = props;

  const followUser = () => {
    subscribeToChannel(getUserSocialChannel(laoId, publicKey))
      .catch((error) => {
        console.error(`Could not subscribe to channel of user ${publicKey.valueOf()}, error: ${error}`);
      });
    setIsFollowing(true);
  };

  // TODO: Navigates to the profile of the user
  const goToUserProfile = () => {};

  return (
    <View style={styles.container}>
      <View style={styles.leftView}>
        <Ionicons name="person" size={40} color="black" />
      </View>
      <View style={styles.rightView}>
        <Text style={styles.publicKeyText}>{publicKey.valueOf()}</Text>
        <View style={styles.buttonsView}>
          <View style={styles.buttonView}>
            <WideButtonView
              title={STRINGS.follow_button}
              onPress={followUser}
              disabled={isFollowing}
            />
          </View>
          <View style={styles.buttonView}>
            <WideButtonView
              title={STRINGS.profile_button}
              onPress={goToUserProfile}
              disabled={!isFollowing}
            />
          </View>
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

UserListItem.prototype = propTypes;

type IPropTypes = {
  laoId: Hash,
  publicKey: PublicKey,
};

export default UserListItem;
