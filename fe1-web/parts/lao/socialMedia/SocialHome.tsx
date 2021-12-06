import React, { useEffect, useState } from 'react';
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
import { PublicKey, RollCall } from 'model/objects';
import { generateToken } from 'model/objects/wallet/Token';
import {
  getKeyPairState, getStore, makeCurrentLao, makeEventGetter,
} from 'store';

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

  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);
  let userPublicKey: PublicKey | undefined;

  if (lao === undefined) {
    throw new Error('LAO is currently undefined, impossible to access to Social Media');
  }

  // If the current user is the organizer, return his public key
  const publicKeyString = getKeyPairState(getStore().getState()).keyPair?.publicKey;
  if (publicKeyString && publicKeyString === lao.organizer.valueOf()) {
    userPublicKey = new PublicKey(publicKeyString);
  }

  // Otherwise, get the pop token of the attendee using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  console.log('Attendee');
  const eventSelect = makeEventGetter(lao.id, rollCallId);
  const rollCall: RollCall = useSelector(eventSelect) as RollCall;

  // This will be run again each time the lao.last_tokenized_roll_call_id changes
  useEffect(() => {
    console.log('useEffects runs');
    generateToken(lao.id, rollCallId).then((token) => {
      if (rollCall.containsToken(token)) {
        userPublicKey = token.publicKey;
      }
    });
  }, [lao.last_tokenized_roll_call_id]);

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
