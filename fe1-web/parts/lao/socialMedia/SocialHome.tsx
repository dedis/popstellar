import React, { useState } from 'react';
import {
  FlatList, StyleSheet, TextStyle, View, ViewStyle,
} from 'react-native';

import TextBlock from 'components/TextBlock';
import TextInputChirp from 'components/TextInputChirp';
import ChirpCard from 'components/ChirpCard';
import STRINGS from 'res/strings';

import { requestAddChirp } from 'network/MessageApi';
import { makeChirpsList } from 'store/reducers/SocialReducer';
import { useSelector } from 'react-redux';
import { KeyPairStore, OpenedLaoStore } from 'store';
import { getCurrentPopToken } from 'model/objects/wallet';
import { PublicKey } from 'model/objects';

/**
 * UI for the Social Media component
 */
const styles = StyleSheet.create({
  view: {
    alignItems: 'center',
  } as ViewStyle,
  textInput: {
    padding: 10,
    borderWidth: 1,
    width: 500,
    alignContent: 'flex-end',
  } as TextStyle,
});

const SocialHome = () => {
  const [inputChirp, setInputChirp] = useState('');

  const currentLao = OpenedLaoStore.get();
  const isOrganizer = (KeyPairStore.getPublicKey() === currentLao.organizer);
  let userPublicKey: PublicKey | undefined;

  // If the current user is an organizer, simply get his public key from KeyPairStore
  if (isOrganizer) {
    userPublicKey = KeyPairStore.getPublicKey();
  } else {
    // If the current user is an attendee, we need to get his pop token
    getCurrentPopToken(currentLao.id).catch((err) => {
      console.error('Could not get pop token of user to send a chirp, error:', err);
    }).then((token) => {
      if (token) {
        userPublicKey = token.publicKey;
      } else {
        console.error('Sending a chirp is impossible: no token found for current user');
      }
    });
  }

  const publishChirp = () => {
    if (userPublicKey) {
      requestAddChirp(userPublicKey, inputChirp)
        .catch((err) => {
          console.error('Could not add chirp, error:', err);
        });
    }
  };

  const chirps = makeChirpsList();
  const chirpList = useSelector(chirps);

  const renderChirpState = ({ item }) => (
    <ChirpCard
      sender={item.sender}
      text={item.text}
      time={item.time}
      likes={item.likes}
    />
  );

  return (
    <View style={styles.view}>
      <TextInputChirp
        onChangeText={setInputChirp}
        onPress={publishChirp}
      />
      <TextBlock text={STRINGS.feed_description} />
      <FlatList
        data={chirpList}
        renderItem={renderChirpState}
        keyExtractor={(item) => item.id.toString()}
      />
    </View>
  );
};

export default SocialHome;
