import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const ScanIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-scan" size={size} color={color} />
);

ScanIcon.propTypes = iconPropTypes;

export default ScanIcon;
