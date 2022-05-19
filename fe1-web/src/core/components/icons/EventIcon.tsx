import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const EventIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-calendar" size={size} color={color} />
);

EventIcon.propTypes = iconPropTypes;
EventIcon.defaultProps = iconDefaultProps;

export default EventIcon;
