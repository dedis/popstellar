import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  StyleSheet, ViewStyle, TextInput, TextStyle, Button, View,
} from 'react-native';
import STRINGS from 'res/strings';
import TextBlock from './TextBlock';

const MAX_CHIRP_CHARS = 300;

/**
 * This is a TextInput component for chirp posting, which counts the number of characters
 * to be sure it doesn't exceed our max value.
 */

const styles = StyleSheet.create({
  viewPublishChirp: {
    marginTop: 50,
    justifyContent: 'center',
  } as ViewStyle,
  textInput: {
    padding: 10,
    borderWidth: 1,
    width: 600,
    alignContent: 'flex-end',
  } as TextStyle,
  buttonView: {
    alignSelf: 'flex-end',
    flexDirection: 'row',
  } as ViewStyle,
});

const TextInputChirp = (props: IPropTypes) => {
  const { placeholder } = props;
  const { numberOfLines } = props;
  const { onPress } = props;
  const { onChangeText } = props;

  const [charsLeft, setCharsLeft] = useState(MAX_CHIRP_CHARS);
  const publishIsDisabled = charsLeft < 0;

  return (
    <View style={styles.viewPublishChirp}>
      <TextInput
        placeholder={placeholder}
        multiline
        numberOfLines={numberOfLines}
        style={styles.textInput}
        onChangeText={(input: string) => {
          onChangeText(input);
          setCharsLeft(MAX_CHIRP_CHARS - input.length);
        }}
      />
      <View style={styles.buttonView}>
        {publishIsDisabled ? <TextBlock text={charsLeft.toString()} color="red" />
          : <TextBlock text={charsLeft.toString()} />}
        <Button
          title={STRINGS.button_publish}
          onPress={() => onPress()}
          disabled={publishIsDisabled}
        />
      </View>
    </View>
  );
};

const propTypes = {
  placeholder: PropTypes.string,
  numberOfLines: PropTypes.number,
  onPress: PropTypes.func.isRequired,
  onChangeText: PropTypes.func.isRequired,
};

TextInputChirp.propTypes = propTypes;

TextInputChirp.defaultProps = {
  placeholder: STRINGS.your_chirp,
  numberOfLines: 4,
};

type IPropTypes = {
  placeholder: string,
  numberOfLines: number,
  onPress: Function,
  onChangeText: Function,
};

export default TextInputChirp;
