import { FontAwesome } from '@expo/vector-icons';
import AntDesign from '@expo/vector-icons/AntDesign';
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
  const { children, onClick } = props;

  return (
    <TouchableOpacity style={circularButtonStyles.roundButton} onPress={onClick}>
      {children}
    </TouchableOpacity>
  );
};

const roundButtonPropTypes = {
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func.isRequired,
};
RoundIconButton.propTypes = roundButtonPropTypes;

type IRoundButtonPropTypes = PropTypes.InferProps<typeof roundButtonPropTypes>;

export const BackRoundButton = (props: IButtonPropTypes) => {
  const { onClick } = props;
  return (
    <RoundIconButton onClick={onClick}>
      <AntDesign
        name="arrowleft"
        size={Buttons.defaultButtonIconSize}
        color={Buttons.defaultButtonIconColor}
      />
    </RoundIconButton>
  );
};

export const LogoutRoundButton = (props: IButtonPropTypes) => {
  const { onClick } = props;
  return (
    <RoundIconButton onClick={onClick}>
      <AntDesign
        name="logout"
        size={Buttons.defaultButtonIconSize}
        color={Buttons.defaultButtonIconColor}
      />
    </RoundIconButton>
  );
};

export const SendRoundButton = (props: IButtonPropTypes) => {
  const { onClick } = props;
  return (
    <RoundIconButton onClick={onClick}>
      <FontAwesome
        name="dollar"
        size={Buttons.defaultButtonIconSize}
        color={Buttons.defaultButtonIconColor}
      />
    </RoundIconButton>
  );
};

export const CloseRoundButton = (props: IButtonPropTypes) => {
  const { onClick } = props;
  return (
    <RoundIconButton onClick={onClick}>
      <AntDesign
        name="close"
        size={Buttons.defaultButtonIconSize}
        color={Buttons.defaultButtonIconColor}
      />
    </RoundIconButton>
  );
};
const buttonPropTypes = {
  onClick: PropTypes.func.isRequired,
};
type IButtonPropTypes = PropTypes.InferProps<typeof buttonPropTypes>;
