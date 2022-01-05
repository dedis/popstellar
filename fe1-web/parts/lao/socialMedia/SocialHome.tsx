import React, { useEffect, useState } from 'react';
import {
  FlatList,
  ListRenderItemInfo,
  StyleSheet,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';

import ChirpCard from 'components/ChirpCard';
import TextInputChirp from 'components/TextInputChirp';
import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';

import { requestAddChirp } from 'network/MessageApi';
import { makeChirpsList } from 'store/reducers/SocialReducer';
import { useSelector } from 'react-redux';
import { PublicKey, RollCall } from 'model/objects';
import { generateToken } from 'model/objects/wallet/Token';
import { makeCurrentLao, makeEventGetter } from 'store';
import { Chirp, ChirpState } from 'model/objects/Chirp';

/**
 * UI for the Social Media home screen component
 */
const styles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
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

const SocialHome = () => {
  const [inputChirp, setInputChirp] = useState('');
  const [userPublicKey, setUserPublicKey] = useState(new PublicKey(''));

  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  if (lao === undefined) {
    throw new Error('LAO is currently undefined, impossible to access to Social Media');
  }

  // Get the pop token of the user using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  const eventSelect = makeEventGetter(lao.id, rollCallId);
  const rollCall: RollCall = useSelector(eventSelect) as RollCall;

  // This will be run again each time the lao.last_tokenized_roll_call_id changes
  useEffect(() => {
    generateToken(lao.id, rollCallId).then((token) => {
      if (token && rollCall.containsToken(token)) {
        setUserPublicKey(token.publicKey);
      }
    });
  }, [lao.last_tokenized_roll_call_id]);

  const publishChirp = () => {
    requestAddChirp(userPublicKey, inputChirp)
      .catch((err) => {
        console.error('Failed to post chirp, error:', err);
      });
  };

  const chirps = makeChirpsList();
  const chirpList = useSelector(chirps);

  const renderChirpState = ({ item }: ListRenderItemInfo<ChirpState>) => (
    <ChirpCard
      chirp={Chirp.fromState(item)}
    />
  );

  return (
    <View style={styles.viewCenter}>
      <View style={styles.homeTextView}>
        <TextBlock text={STRINGS.social_media_navigation_tab_home} />
      </View>
      <View style={styles.userFeed}>
        <TextInputChirp
          onChangeText={setInputChirp}
          onPress={publishChirp}
          // The publish button is disabled when the user public key is not defined
          publishIsDisabledCond={userPublicKey.valueOf() === ''}
        />
        <FlatList
          data={chirpList}
          renderItem={renderChirpState}
          keyExtractor={(item) => item.id.toString()}
        />
      </View>
    </View>
  );
};

export default SocialHome;
