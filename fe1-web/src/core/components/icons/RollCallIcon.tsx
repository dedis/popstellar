import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const RollCallIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-hand-left" size={size} color={color} />
);

RollCallIcon.propTypes = iconPropTypes;
RollCallIcon.defaultProps = iconDefaultProps;

export default RollCallIcon;
