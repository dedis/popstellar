import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput, View } from 'react-native';

import { Border, Color, Spacing, Typography } from 'core/styles';

const styles = StyleSheet.create({
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
  const { value, placeholder, onChange, enabled, negative, testID } = props;

  const inputStyles = [Typography.paragraph, styles.input];

  if (!enabled) {
    inputStyles.push(styles.disabled);
  }

  if (negative) {
    inputStyles.push(styles.negative);
  }

  return (
    <View style={styles.container}>
      <TextInput
        style={inputStyles}
        placeholderTextColor={Color.inactive}
        editable={enabled || false}
        value={value}
        placeholder={placeholder || ''}
        onChangeText={enabled ? onChange : undefined}
        testID={testID || undefined}
      />
    </View>
  );
};

const propTypes = {
  placeholder: PropTypes.string,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  enabled: PropTypes.bool,
  negative: PropTypes.bool,
  testID: PropTypes.string,
};
Input.propTypes = propTypes;
Input.defaultProps = {
  placeholder: '',
  onChange: undefined,
  enabled: true,
  negative: false,
  testID: undefined,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
};

export default Input;
