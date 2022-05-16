import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const CloseIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-close" size={size} color={color} />
);

CloseIcon.propTypes = iconPropTypes;
CloseIcon.defaultProps = iconDefaultProps;

export default CloseIcon;
