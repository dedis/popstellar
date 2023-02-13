import { Ionicons } from '@expo/vector-icons';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, TextStyle, View, ViewStyle } from 'react-native';

import { PoPTextButton, ProfileIcon } from 'core/components';
import { PublicKey } from 'core/objects';
import { Border, Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

const MAX_CHIRP_CHARS = 300;

/**
 * This is a TextInput component for chirp posting, which counts the number of characters
 * to be sure it doesn't exceed our max value.
 */

const styles = StyleSheet.create({
  container: {
    borderColor: Color.primary,
    borderRadius: Border.radius,
    backgroundColor: Color.contrast,
    flexDirection: 'row',
    padding: Spacing.contentSpacing,
  } as ViewStyle,
  leftView: {
    marginRight: Spacing.x1,
  } as ViewStyle,
  rightView: {
    display: 'flex',
    flexDirection: 'column',
    flexGrow: 1,
  } as ViewStyle,
  textInput: {
    alignContent: 'flex-end',
    padding: Spacing.x1,
    borderColor: Color.primary,
    borderRadius: Border.radius,
    borderWidth: Border.width,
  } as TextStyle,
  buttonView: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-end',
    marginTop: Spacing.x1,
  } as ViewStyle,
  charsLeft: {
    marginRight: Spacing.x1,
  } as ViewStyle,
});

const TextInputChirp = (props: IPropTypes) => {
  const {
    value,
    placeholder,
    numberOfLines,
    onPress,
    onChangeText,
    disabled: alwaysDisabled,
    currentUserPublicKey,
    testID,
  } = props;

  const [focused, setFocused] = useState(false);
  const charsLeft = MAX_CHIRP_CHARS - value.length;
  const textIsRed = charsLeft < 0;
  const disabled = textIsRed || charsLeft === MAX_CHIRP_CHARS || alwaysDisabled;

  const showFullInput = focused || charsLeft !== MAX_CHIRP_CHARS;

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
          placeholder={placeholder || undefined}
          multiline
          selectTextOnFocus
          numberOfLines={(showFullInput ? numberOfLines : 1) || undefined}
          style={[Typography.base, styles.textInput]}
          placeholderTextColor={Color.inactive}
          onChangeText={onChangeText}
          testID={testID ? `${testID}_input` : undefined}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
        />
        <View style={styles.buttonView}>
          <View style={styles.charsLeft}>
            <Text style={textIsRed ? [Typography.base, Typography.error] : [Typography.base]}>
              {charsLeft.toString()}
            </Text>
          </View>
          <PoPTextButton
            onPress={onPress}
            disabled={disabled === true}
            testID={testID ? `${testID}_publish` : undefined}
            toolbar>
            {STRINGS.button_publish}
          </PoPTextButton>
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
  disabled: PropTypes.bool,
  currentUserPublicKey: PropTypes.instanceOf(PublicKey),
  testID: PropTypes.string,
};

TextInputChirp.propTypes = propTypes;

TextInputChirp.defaultProps = {
  placeholder: STRINGS.your_chirp,
  numberOfLines: 5,
  disabled: false,
  currentUserPublicKey: undefined,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextInputChirp;
