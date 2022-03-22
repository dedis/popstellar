import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput, TextStyle, View, ViewStyle } from 'react-native';

import { Typography, Views } from '../styles';
import CopyButton from './CopyButton';

/**
 * This is a TextInput component which data is copiable
 * to clipboard by clicking the copy button.
 */

const styles = StyleSheet.create({
  view: {
    ...Views.base,
    flexDirection: 'row',
    zIndex: 3,
  } as ViewStyle,
  textInput: {
    ...Typography.base,
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
    width: 750,
  } as TextStyle,
});

const CopiableTextInput = (props: IPropTypes) => {
  const { text } = props;

  return (
    <View style={styles.view}>
      <TextInput value={text} style={styles.textInput} selectTextOnFocus />
      <CopyButton data={text} />
    </View>
  );
};

const propTypes = {
  text: PropTypes.string,
};

CopiableTextInput.propTypes = propTypes;

CopiableTextInput.defaultProps = {
  text: '',
};

type IPropTypes = {
  text: string;
};

export default CopiableTextInput;
