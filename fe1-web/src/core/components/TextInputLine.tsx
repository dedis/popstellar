import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput, TextStyle } from 'react-native';

import { Border, Color, Spacing, Typography } from '../styles';

/**
 * Component which creates the typical one line text input used in the application.
 * When we click on it, the whole text is selected to allow easier rewriting.
 */

const styles = StyleSheet.create({
  textInput: {
    ...Typography.base,
    color: Color.primary,
    borderBottomWidth: Border.width,
    borderColor: Color.primary,
    marginVertical: Spacing.x05,
    padding: Spacing.x05,
  } as TextStyle,
});

const TextInputLine = (props: IPropTypes) => {
  const { onChangeText, placeholder, defaultValue } = props;

  return (
    <TextInput
      style={styles.textInput}
      onChangeText={onChangeText}
      placeholder={placeholder || ''}
      placeholderTextColor={Color.inactive}
      defaultValue={defaultValue || ''}
      selectTextOnFocus
    />
  );
};

const propTypes = {
  onChangeText: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  defaultValue: PropTypes.string,
};
TextInputLine.propTypes = propTypes;

TextInputLine.defaultProps = {
  placeholder: '',
  defaultValue: '',
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextInputLine;
