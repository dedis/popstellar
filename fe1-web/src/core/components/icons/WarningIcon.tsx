import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const WarningIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-warning" size={size} color={color} />
);

WarningIcon.propTypes = iconPropTypes;

export default WarningIcon;
