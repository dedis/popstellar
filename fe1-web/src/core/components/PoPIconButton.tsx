import PropTypes from 'prop-types';
import React from 'react';
import { TextStyle } from 'react-native';

import { ExtendType } from 'core/types';

import { Color, Icon, Typography } from '../styles';
import PoPButton from './PoPButton';
import PoPIcon, { PopIconName } from './PoPIcon';

const PoPIconButton = (props: IPropTypes) => {
  const { onPress, buttonStyle, disabled, negative, toolbar, testID, name } = props;

  const textStyles: TextStyle[] = [Typography.base, Typography.centered, Typography.negative];

  if (buttonStyle === 'secondary') {
    // in case of an outlined button, the text color
    // should be the same as the border's
    if (disabled) {
      textStyles.push(Typography.inactive);
    } else if (negative) {
      textStyles.push(Typography.negative);
    } else {
      textStyles.push(Typography.accent);
    }
  }

  return (
    <PoPButton
      onPress={onPress}
      buttonStyle={buttonStyle}
      disabled={disabled}
      negative={negative}
      toolbar={toolbar}
      testID={testID}>
      <PoPIcon name={name} size={Icon.size} color={Color.contrast} />
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
  // changes background color / border color to be white
  // disabled takes precedence though!
  negative: PropTypes.bool,
  // makes the button placement work in the toolbar
  toolbar: PropTypes.bool,
  name: PropTypes.string.isRequired,
  testID: PropTypes.string,
};
PoPIconButton.propTypes = propTypes;

PoPIconButton.defaultProps = {
  buttonStyle: 'primary',
  disabled: false,
  negative: false,
  toolbar: false,
  testID: undefined,
};

type IPropTypes = ExtendType<PropTypes.InferProps<typeof propTypes>, { name: PopIconName }>;

export default PoPIconButton;
