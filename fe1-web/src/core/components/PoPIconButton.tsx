import PropTypes from 'prop-types';
import React from 'react';

import { ExtendType } from 'core/types';

import { Color, Icon } from '../styles';
import PoPButton from './PoPButton';
import PoPIcon, { PopIconName } from './PoPIcon';

const SIZE_MAP = {
  normal: Icon.size,
  small: Icon.smallSize,
};

const PoPIconButton = (props: IPropTypes) => {
  const { onPress, buttonStyle, disabled, toolbar, size, testID, name } = props;

  return (
    <PoPButton
      onPress={onPress}
      buttonStyle={buttonStyle}
      disabled={disabled}
      toolbar={toolbar}
      testID={testID}>
      <PoPIcon
        name={name}
        size={SIZE_MAP[size]}
        color={buttonStyle === 'primary' ? Color.contrast : Color.accent}
      />
    </PoPButton>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  // primary: colored background, negative text
  // secondary: outlined button
  buttonStyle: PropTypes.oneOf<'primary' | 'secondary'>(['primary', 'secondary']),
  size: PropTypes.oneOf<'normal' | 'small'>(['normal', 'small']),
  // changes background color / border color to be gray
  disabled: PropTypes.bool,
  // makes the button placement work in the toolbar
  toolbar: PropTypes.bool,
  name: PropTypes.string.isRequired,
  testID: PropTypes.string,
};
PoPIconButton.propTypes = propTypes;

PoPIconButton.defaultProps = {
  buttonStyle: 'primary',
  size: 'normal',
  disabled: false,
  toolbar: false,
  testID: undefined,
};

type IPropTypes = ExtendType<
  PropTypes.InferProps<typeof propTypes>,
  { name: PopIconName; size: 'normal' | 'small' }
>;

export default PoPIconButton;
