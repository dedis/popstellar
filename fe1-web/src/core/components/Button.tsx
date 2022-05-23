import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { TouchableOpacity } from 'react-native-gesture-handler';

import { Border, Color, Spacing } from '../styles';

const styles = StyleSheet.create({
  container: {
    width: '100%',
    marginBottom: Spacing.x1,
  } as ViewStyle,
  button: {
    padding: Spacing.x05,

    borderColor: Color.accent,
    backgroundColor: Color.accent,
    borderWidth: Border.width,
    borderRadius: Border.radius,
  } as ViewStyle,
  negative: {
    borderColor: Color.contrast,
  } as ViewStyle,
});

const Button = (props: IPropTypes) => {
  const { onPress, disabled, children, negative } = props;

  return (
    <TouchableOpacity containerStyle={styles.container} onPress={disabled ? undefined : onPress}>
      <View style={negative ? [styles.button, styles.negative] : styles.button}>{children}</View>
    </TouchableOpacity>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  negative: PropTypes.bool,
  children: PropTypes.node,
};
Button.propTypes = propTypes;

Button.defaultProps = {
  disabled: false,
  negative: false,
  children: null,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Button;
