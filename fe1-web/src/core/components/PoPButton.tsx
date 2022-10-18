import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Border, Color, Spacing } from '../styles';
import PoPTouchableOpacity from './PoPTouchableOpacity';

const styles = StyleSheet.create({
  container: {
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
  outline: {
    borderColor: Color.accent,
    backgroundColor: Color.contrast,
  } as ViewStyle,
  negativeBorder: {
    borderColor: Color.contrast,
  } as ViewStyle,
  disabledNegativeBorder: {
    borderColor: Color.inactive,
    backgroundColor: Color.inactive,
  } as ViewStyle,
});

const PoPButton = (props: IPropTypes) => {
  const { onPress, disabled, children, outline, negativeBorder, margin, testID } = props;

  const viewStyles = [styles.button];
  if (negativeBorder && disabled) {
    viewStyles.push(styles.disabledNegativeBorder);
  } else if (negativeBorder) {
    viewStyles.push(styles.negativeBorder);
  } else if (disabled) {
    viewStyles.push(styles.disabled);
  } else if (outline) {
    viewStyles.push(styles.outline);
  }

  return (
    <PoPTouchableOpacity
      containerStyle={margin ? styles.container : []}
      onPress={disabled ? undefined : onPress}
      testID={testID || undefined}>
      <View style={viewStyles}>{children}</View>
    </PoPTouchableOpacity>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  outline: PropTypes.bool,
  negativeBorder: PropTypes.bool,
  margin: PropTypes.bool,
  children: PropTypes.node,
  testID: PropTypes.string,
};
PoPButton.propTypes = propTypes;

PoPButton.defaultProps = {
  disabled: false,
  outline: false,
  negativeBorder: false,
  margin: true,
  children: null,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPButton;
