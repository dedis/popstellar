import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const DeleteIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-trash" size={size} color={color} />
);

DeleteIcon.propTypes = iconPropTypes;
DeleteIcon.defaultProps = iconDefaultProps;

export default DeleteIcon;
