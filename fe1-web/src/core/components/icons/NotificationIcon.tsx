import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const NotificationIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-notifications" size={size} color={color} />
);

NotificationIcon.propTypes = iconPropTypes;
NotificationIcon.defaultProps = iconDefaultProps;

export default NotificationIcon;
