import PropTypes from 'prop-types';
import React from 'react';
import { TouchableOpacity } from 'react-native';

import { Buttons } from '../styles';
import circularButtonStyles from '../styles/stylesheets/circularButtonStyles';

/**
 * This is a round button with an icon that uses expo icons
 * Ref: https://icons.expo.fyi/
 */
const RoundIconButton = (props: IRoundButtonPropTypes) => {
  const { type, name, onClick } = props;
  const family = type || Buttons.defaultButtonIconFamily;
  return (
    <TouchableOpacity style={circularButtonStyles.roundButton} onPress={onClick}>
      {React.createElement(family, {
        name: name,
        size: Buttons.defaultButtonIconSize,
        color: Buttons.defaultButtonIconColor,
      })}
    </TouchableOpacity>
  );
};

const roundButtonPropTypes = {
  type: PropTypes.string,
  name: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
};
type IRoundButtonPropTypes = PropTypes.InferProps<typeof roundButtonPropTypes>;

export const BackRoundButton = (props: IButtonPropTypes) => {
  const { onClick } = props;
  return <RoundIconButton name="arrowleft" onClick={onClick} />;
};

export const LogoutRoundButton = (props: IButtonPropTypes) => {
  const { onClick } = props;
  return <RoundIconButton name="logout" onClick={onClick} />;
};

const buttonPropTypes = {
  onClick: PropTypes.func.isRequired,
};
type IButtonPropTypes = PropTypes.InferProps<typeof buttonPropTypes>;
