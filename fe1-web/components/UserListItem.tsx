import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  View, Text, StyleSheet, ViewStyle, TextStyle,
} from 'react-native';
import { getUserSocialChannel, PublicKey } from 'model/objects';
import { gray } from 'styles/colors';
import { Ionicons } from '@expo/vector-icons';
import STRINGS from 'res/strings';
import { subscribeToChannel } from 'network/CommunicationApi';
import { makeCurrentLao } from 'store';
import { useSelector } from 'react-redux';
import WideButtonView from './WideButtonView';

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
  const { publicKey } = props;
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  const followUser = () => {
    subscribeToChannel(getUserSocialChannel(lao!!.id, publicKey.valueOf()))
      .catch((error) => {
        console.error(`Could not subscribe to channel of user ${publicKey.valueOf()}, error: ${error}`);
      });
    setIsFollowing(true);
  };

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
              onPress={() => {}}
              disabled={!isFollowing}
            />
          </View>
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

UserListItem.prototype = propTypes;

type IPropTypes = {
  publicKey: PublicKey,
};

export default UserListItem;
