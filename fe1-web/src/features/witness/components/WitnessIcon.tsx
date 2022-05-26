import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import {
  iconDefaultProps,
  iconPropTypes,
  IconPropTypes,
} from 'core/components/icons/IconPropTypes';

const WitnessIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-eye" size={size} color={color} />
);

WitnessIcon.propTypes = iconPropTypes;
WitnessIcon.defaultProps = iconDefaultProps;

export default WitnessIcon;
