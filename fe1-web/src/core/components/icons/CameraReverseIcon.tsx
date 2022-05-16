import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const CameraReverseIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-camera-reverse" size={size} color={color} />
);

CameraReverseIcon.propTypes = iconPropTypes;

export default CameraReverseIcon;
