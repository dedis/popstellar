import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const SocialMediaIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-people" size={size} color={color} />
);

SocialMediaIcon.propTypes = iconPropTypes;
SocialMediaIcon.defaultProps = iconDefaultProps;

export default SocialMediaIcon;
