import React from 'react';
import {
  StyleSheet, TextInput, TextStyle, View, ViewStyle,
} from 'react-native';

import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';
import WideButtonView from 'components/WideButtonView';

const maxChirpChars = 280;

/**
 * UI for the Social Media component
 */
const styles = StyleSheet.create({
  view: {
    alignItems: 'center',
  } as ViewStyle,
  viewPublishChirp: {
    marginTop: 50,
    justifyContent: 'center',
  } as ViewStyle,
  textInput: {
    padding: 10,
    borderWidth: 1,
    width: 500,
    alignContent: 'flex-end',
  } as TextStyle,
});

let charCounter = 0;
let charsLeft = maxChirpChars;

// TODO: Implement this method, so that it sends a test chirp to the network
const publishChirp = () => {};

const onChangeText = (text: String) => {
  charCounter = text.length;
  charsLeft = maxChirpChars - charCounter;
};

const Social = () => (
  <View style={styles.view}>
    <View style={styles.viewPublishChirp}>
      <TextInput
        placeholder={STRINGS.your_chirp}
        multiline
        numberOfLines={4}
        style={styles.textInput}
        onChange={onChangeText}
      />
      <TextBlock text={charsLeft.toString()} />
      <WideButtonView
        title={STRINGS.button_publish}
        onPress={publishChirp}
      />
    </View>
    <TextBlock text={STRINGS.feed_description} />
  </View>
);

export default Social;
