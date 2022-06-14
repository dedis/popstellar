import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Border, Color, Spacing } from '../styles';
import PoPTouchableOpacity from './PoPTouchableOpacity';

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
  disabled: {
    borderColor: Color.inactive,
    backgroundColor: Color.inactive,
  } as ViewStyle,
  negative: {
    borderColor: Color.contrast,
  } as ViewStyle,
  disabledNegative: {
    borderColor: Color.inactive,
    backgroundColor: Color.inactive,
  } as ViewStyle,
});

const PoPButton = (props: IPropTypes) => {
  const { onPress, disabled, children, negative, testID } = props;

  const viewStyles = [styles.button];
  if (negative && disabled) {
    viewStyles.push(styles.disabledNegative);
  } else if (negative) {
    viewStyles.push(styles.negative);
  } else if (disabled) {
    viewStyles.push(styles.disabled);
  }

  return (
    <PoPTouchableOpacity
      containerStyle={styles.container}
      onPress={disabled ? undefined : onPress}
      testID={testID || undefined}>
      <View style={viewStyles}>{children}</View>
    </PoPTouchableOpacity>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  negative: PropTypes.bool,
  children: PropTypes.node,
  testID: PropTypes.string,
};
PoPButton.propTypes = propTypes;

PoPButton.defaultProps = {
  disabled: false,
  negative: false,
  children: null,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPButton;
