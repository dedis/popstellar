import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const SettingsIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-cog" size={size} color={color} />
);

SettingsIcon.propTypes = iconPropTypes;

export default SettingsIcon;
