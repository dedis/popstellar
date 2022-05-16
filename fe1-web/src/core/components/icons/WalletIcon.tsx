import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const WalletIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-wallet" size={size} color={color} />
);

WalletIcon.propTypes = iconPropTypes;
WalletIcon.defaultProps = iconDefaultProps;

export default WalletIcon;
