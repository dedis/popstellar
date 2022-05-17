import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const CodeIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-code" size={size} color={color} />
);

CodeIcon.propTypes = iconPropTypes;

export default CodeIcon;
