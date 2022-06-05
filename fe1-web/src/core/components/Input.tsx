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
    padding: Spacing.x05,
  },
  border: {
    borderWidth: 1,
    borderColor: Color.primary,
  },
  disabled: {
    color: Color.gray,
  },
});

const Input = (props: IPropTypes) => {
  const { value, placeholder, onChange, enabled, border, testID } = props;

  const inputStyles = [Typography.paragraph, styles.input];
  if (!enabled) {
    inputStyles.push(styles.disabled);
  }

  if (border) {
    inputStyles.push(styles.border);
  }
  return (
    <View style={styles.container}>
      <TextInput
        style={inputStyles}
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
  enabled: PropTypes.bool,
  border: PropTypes.bool,
  placeholder: PropTypes.string,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  testID: PropTypes.string,
};
Input.propTypes = propTypes;
Input.defaultProps = {
  placeholder: '',
  enabled: true,
  border: false,
  onChange: undefined,
  testID: undefined,
};

type IPropTypes = Omit<PropTypes.InferProps<typeof propTypes>, 'onChange'> & {
  onChange: (value: string) => void;
};

export default Input;
