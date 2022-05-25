import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const MeetingIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-calendar" size={size} color={color} />
);

MeetingIcon.propTypes = iconPropTypes;
MeetingIcon.defaultProps = iconDefaultProps;

export default MeetingIcon;
