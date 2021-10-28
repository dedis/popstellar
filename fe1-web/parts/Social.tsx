import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import styleContainer from 'styles/stylesheets/container';

import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';
import WideButtonView from 'components/WideButtonView';

import { requestAddChirp } from 'network/MessageApi';

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
    requestAddChirp('chirp chirp')
      .catch((err) => {
        console.error('Could not add chirp, error:', err);
      });
    console.log(inputChirp);
  };

const Social = () => (
  <View style={styles.view}>
      <TextInputChirp
        onChangeText={setInputChirp}
        onPress={publishChirp}
      />
      <TextBlock text={STRINGS.feed_description} />
  </View>
);

export default Social;
