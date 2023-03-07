import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput, View } from 'react-native';

import { Border, Color, Spacing, Typography } from 'core/styles';

export const inputStyleSheet = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row',
  },
  input: {
    // this makes the input field shrink down to a width of 50
    width: 50,
    flex: 1,
    backgroundColor: Color.contrast,
    borderRadius: Border.inputRadius,
    borderWidth: Border.width,
    borderColor: Color.contrast,
    padding: Spacing.x05,
  },
  negative: {
    ...Typography.negative,
    ...Border.negativeColor,
    backgroundColor: Color.accent,
  },
  disabled: {
    color: Color.gray,
  },
});

const Input = (props: IPropTypes) => {
  const { value, placeholder, onChange, onFocus, onBlur, enabled, negative, testID, isMonospaced } =
    props;

  const inputStyles = [Typography.paragraph, inputStyleSheet.input];

  if (isMonospaced) {
    inputStyles.push(Typography.code);
  }

  if (!enabled) {
    inputStyles.push(inputStyleSheet.disabled);
  }

  if (negative) {
    inputStyles.push(inputStyleSheet.negative);
  }

  return (
    <View style={inputStyleSheet.container}>
      <TextInput
        style={inputStyles}
        placeholderTextColor={Color.inactive}
        editable={enabled || false}
        value={value}
        placeholder={placeholder || ''}
        onChangeText={enabled ? onChange : undefined}
        onFocus={onFocus || undefined}
        onBlur={onBlur || undefined}
        testID={testID || undefined}
      />
    </View>
  );
};

const propTypes = {
  placeholder: PropTypes.string,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  onFocus: PropTypes.func,
  onBlur: PropTypes.func,
  enabled: PropTypes.bool,
  negative: PropTypes.bool,
  testID: PropTypes.string,
  isMonospaced: PropTypes.bool,
};
Input.propTypes = propTypes;
Input.defaultProps = {
  placeholder: '',
  onChange: undefined,
  onFocus: undefined,
  onBlur: undefined,
  enabled: true,
  negative: false,
  testID: undefined,
  isMonospaced: false,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
};

export default Input;
