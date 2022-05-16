import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const ScanIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-scan" size={size} color={color} />
);

ScanIcon.propTypes = iconPropTypes;
ScanIcon.defaultProps = iconDefaultProps;

export default ScanIcon;
