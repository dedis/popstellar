import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Border, Color, Spacing } from '../styles';
import PoPTouchableOpacity from './PoPTouchableOpacity';

const styles = StyleSheet.create({
  toolbar: {} as ViewStyle,
  containerMargin: {
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
    backgroundColor: Color.transparent,
  } as ViewStyle,
});

const PoPButton = (props: IPropTypes) => {
  const { onPress, buttonStyle, disabled, toolbar, testID, children } = props;

  const viewStyles = [styles.button];

  // background color / border color
  if (disabled) {
    viewStyles.push(styles.disabled);
  }

  // secondary button style removes background color
  if (buttonStyle === 'secondary') {
    viewStyles.push(styles.outline);
  }

  return (
    <PoPTouchableOpacity
      containerStyle={toolbar ? styles.toolbar : styles.containerMargin}
      onPress={disabled ? undefined : onPress}
      testID={testID || undefined}>
      <View style={viewStyles}>{children}</View>
    </PoPTouchableOpacity>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  // primary: colored background, negative text
  // secondary: outlined button
  buttonStyle: PropTypes.oneOf<'primary' | 'secondary'>(['primary', 'secondary']),
  // changes background color / border color to be gray
  disabled: PropTypes.bool,
  // makes the button placement work in the toolbar
  toolbar: PropTypes.bool,
  children: PropTypes.node,
  testID: PropTypes.string,
};
PoPButton.propTypes = propTypes;

PoPButton.defaultProps = {
  buttonStyle: 'primary',
  disabled: false,
  toolbar: false,
  children: null,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPButton;
