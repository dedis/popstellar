import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const HomeIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-home" size={size} color={color} />
);

HomeIcon.propTypes = iconPropTypes;

export default HomeIcon;
