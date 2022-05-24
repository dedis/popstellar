import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TextInput } from 'react-native';

import { Border, Color, Spacing, Typography } from 'core/styles';

const styles = StyleSheet.create({
  input: {
    flex: 1,
    backgroundColor: Color.contrast,
    borderRadius: Border.inputRadius,
    padding: Spacing.x05,
  },
  disabled: {
    color: Color.gray,
  },
});

const Input = (props: IPropTypes) => {
  const { value, placeholder, onChange, enabled } = props;
  return (
    <TextInput
      style={
        enabled
          ? [Typography.paragraph, styles.input]
          : [Typography.paragraph, styles.input, styles.disabled]
      }
      editable={enabled || false}
      value={value}
      placeholder={placeholder || ''}
      onChangeText={enabled ? onChange : undefined}
    />
  );
};

const propTypes = {
  enabled: PropTypes.bool,
  placeholder: PropTypes.string,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func,
};
Input.propTypes = propTypes;
Input.defaultProps = {
  placeholder: '',
  enabled: true,
  onChange: undefined,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
};

export default Input;
