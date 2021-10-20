import React, { useState } from 'react';
import {
  StyleSheet, TextStyle, View, ViewStyle,
} from 'react-native';

import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';
import TextInputChirp from '../components/TextInputChirp';

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

  // TODO: Implement this method, so that it sends a test chirp to the network
  const publishChirp = () => {
    console.log(inputChirp.length);
  };

  return (
    <View style={styles.view}>
      <TextInputChirp
        onChangeText={setInputChirp}
        onPress={publishChirp}
      />
      <TextBlock text={STRINGS.feed_description} />
    </View>
  );
};

export default Social;
