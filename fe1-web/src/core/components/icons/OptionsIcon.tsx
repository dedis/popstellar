import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const OptionsIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-ellipsis-horizontal" size={size} color={color} />
);

OptionsIcon.propTypes = iconPropTypes;

export default OptionsIcon;
