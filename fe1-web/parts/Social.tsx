import React, { useState } from 'react';
import {
  FlatList,
  StyleSheet, TextStyle, View, ViewStyle,
} from 'react-native';

import TextBlock from 'components/TextBlock';
import TextInputChirp from 'components/TextInputChirp';
import STRINGS from 'res/strings';

import { requestAddChirp } from 'network/MessageApi';
import { makeChirpsList } from 'store/reducers/SocialReducer';
import { useSelector } from 'react-redux';

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

const Social = () => {
  const [inputChirp, setInputChirp] = useState('');

  const publishChirp = () => {
    requestAddChirp(inputChirp)
      .catch((err) => {
        console.error('Could not add chirp, error:', err);
      });
  };

  const chirps = makeChirpsList();
  const chirpList = useSelector(chirps);

  const renderChirpState = ({ item }) => {
    const text = `${item.sender} said ${item.text}`;
    return <TextBlock text={text} />;
  };

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

export default Social;
