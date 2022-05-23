import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const DropdownIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-chevron-down" size={size} color={color} />
);

DropdownIcon.propTypes = iconPropTypes;
DropdownIcon.defaultProps = iconDefaultProps;

export default DropdownIcon;
