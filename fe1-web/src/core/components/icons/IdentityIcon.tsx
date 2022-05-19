import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const IdentityIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-person" size={size} color={color} />
);

IdentityIcon.propTypes = iconPropTypes;
IdentityIcon.defaultProps = iconDefaultProps;

export default IdentityIcon;
