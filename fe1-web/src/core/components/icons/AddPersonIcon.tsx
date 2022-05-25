import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const AddPersonIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-person-add" size={size} color={color} />
);

AddPersonIcon.propTypes = iconPropTypes;
AddPersonIcon.defaultProps = iconDefaultProps;

export default AddPersonIcon;
