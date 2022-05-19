import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const ListIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-list" size={size} color={color} />
);

ListIcon.propTypes = iconPropTypes;
ListIcon.defaultProps = iconDefaultProps;

export default ListIcon;
