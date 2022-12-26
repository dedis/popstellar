import PropTypes from 'prop-types';
import React from 'react';
import { Text, TextStyle } from 'react-native';

import { Typography } from '../styles';
import PoPButton from './PoPButton';

const PoPTextButton = (props: IPropTypes) => {
  const { onPress, buttonStyle, disabled, toolbar, testID, children: text } = props;

  const textStyles: TextStyle[] = [Typography.base, Typography.centered, Typography.negative];

  if (buttonStyle === 'secondary') {
    // in case of an outlined button, the text color
    // should be the same as the border's
    if (disabled) {
      textStyles.push(Typography.inactive);
    } else {
      textStyles.push(Typography.accent);
    }
  }

  return (
    <PoPButton
      onPress={onPress}
      buttonStyle={buttonStyle}
      disabled={disabled}
      toolbar={toolbar}
      testID={testID}>
      <Text style={textStyles}>{text}</Text>
    </PoPButton>
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
  children: PropTypes.string.isRequired,
  testID: PropTypes.string,
};
PoPTextButton.propTypes = propTypes;

PoPTextButton.defaultProps = {
  buttonStyle: 'primary',
  disabled: false,
  toolbar: false,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPTextButton;
