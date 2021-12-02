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
import { PublicKey } from 'model/objects';
import { getCurrentPublicKey } from 'model/objects/wallet/Token';

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

  let userPublicKey: PublicKey | undefined;

  getCurrentPublicKey().then((pk) => {
    userPublicKey = pk;
  });

  const publishChirp = () => {
    if (userPublicKey) {
      requestAddChirp(userPublicKey, inputChirp)
        .catch((err) => {
          console.error('Failed to post chirp, error:', err);
        });
    } else {
      console.error('No token found for current user. '
        + 'Be sure to have participated in a Roll-Call.');
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
