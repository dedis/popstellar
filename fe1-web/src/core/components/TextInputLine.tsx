import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput, TextStyle } from 'react-native';

import { Spacing, Typography } from '../styles';

/**
 * Component which creates the typical one line text input used in the application.
 * When we click on it, the whole text is selected to allow easier rewriting.
 */

const styles = StyleSheet.create({
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
    marginVertical: Spacing.s,
    marginHorizontal: Spacing.xl,
  } as TextStyle,
});

const TextInputLine = (props: IPropTypes) => {
  const { onChangeText } = props;
  let { placeholder } = props;
  let { defaultValue } = props;

  if (placeholder == null) {
    placeholder = '';
  }
  if (defaultValue == null) {
    defaultValue = '';
  }

  return (
    <TextInput
      style={styles.textInput}
      onChangeText={onChangeText}
      placeholder={placeholder}
      defaultValue={defaultValue}
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
