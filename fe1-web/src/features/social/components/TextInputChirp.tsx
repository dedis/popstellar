import { Ionicons } from '@expo/vector-icons';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Button, StyleSheet, TextInput, TextStyle, View, ViewStyle } from 'react-native';

import { ProfileIcon, TextBlock } from 'core/components';
import { PublicKey } from 'core/objects';
import { gray, red } from 'core/styles/color';
import STRINGS from 'resources/strings';

const MAX_CHIRP_CHARS = 300;

/**
 * This is a TextInput component for chirp posting, which counts the number of characters
 * to be sure it doesn't exceed our max value.
 */

const styles = StyleSheet.create({
  container: {
    borderColor: gray,
    borderWidth: 1,
    flexDirection: 'row',
    padding: 10,
    width: 600,
  } as ViewStyle,
  leftView: {
    width: 60,
  } as ViewStyle,
  rightView: {
    display: 'flex',
    flexDirection: 'column',
  } as ViewStyle,
  textInput: {
    fontSize: 18,
    padding: 10,
    width: 520,
    alignContent: 'flex-end',
    borderColor: gray,
    borderWidth: 1,
  } as TextStyle,
  buttonView: {
    fontSize: 18,
    alignSelf: 'flex-end',
    flexDirection: 'row',
  } as ViewStyle,
});

const TextInputChirp = (props: IPropTypes) => {
  const {
    value,
    placeholder,
    numberOfLines,
    onPress,
    onChangeText,
    publishIsDisabledCond,
    currentUserPublicKey,
    testID,
  } = props;

  const [charsLeft, setCharsLeft] = useState(MAX_CHIRP_CHARS);
  const textIsRed = charsLeft < 0;
  const publishIsDisabled = textIsRed || charsLeft === MAX_CHIRP_CHARS || publishIsDisabledCond;

  return (
    <View style={styles.container}>
      <View style={styles.leftView}>
        {
          // If the current user public key is defined, show the profile picture accordingly
          !currentUserPublicKey ? (
            <Ionicons name="person" size={40} color="black" />
          ) : (
            <ProfileIcon publicKey={currentUserPublicKey} />
          )
        }
      </View>
      <View style={styles.rightView}>
        <TextInput
          value={value}
          placeholder={placeholder}
          multiline
          selectTextOnFocus
          numberOfLines={numberOfLines}
          style={styles.textInput}
          onChangeText={(input: string) => {
            onChangeText(input);
            setCharsLeft(MAX_CHIRP_CHARS - input.length);
          }}
          testID={testID ? `${testID}_input` : undefined}
        />
        <View style={styles.buttonView}>
          <TextBlock text={charsLeft.toString()} color={textIsRed ? red : undefined} />
          <Button
            title={STRINGS.button_publish}
            onPress={() => onPress()}
            disabled={publishIsDisabled}
            testID={testID ? `${testID}_publish` : undefined}
          />
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  value: PropTypes.string.isRequired,
  placeholder: PropTypes.string,
  numberOfLines: PropTypes.number,
  onPress: PropTypes.func.isRequired,
  onChangeText: PropTypes.func.isRequired,
  publishIsDisabledCond: PropTypes.bool,
  currentUserPublicKey: PropTypes.instanceOf(PublicKey),
  testID: PropTypes.string,
};

TextInputChirp.propTypes = propTypes;

TextInputChirp.defaultProps = {
  placeholder: STRINGS.your_chirp,
  numberOfLines: 5,
  publishIsDisabledCond: false,
  currentUserPublicKey: undefined,
  testID: undefined,
};

type IPropTypes = {
  value: string;
  placeholder: string;
  numberOfLines: number;
  onPress: Function;
  onChangeText: Function;
  publishIsDisabledCond: boolean;
  currentUserPublicKey: PublicKey;
  testID?: string;
};

export default TextInputChirp;
