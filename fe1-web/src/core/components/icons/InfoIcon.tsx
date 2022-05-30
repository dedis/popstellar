import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const InfoIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-information-circle-outline" size={size} color={color} />
);

InfoIcon.propTypes = iconPropTypes;
InfoIcon.defaultProps = iconDefaultProps;

export default InfoIcon;
