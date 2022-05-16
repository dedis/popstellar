import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconPropTypes, IconPropTypes } from './IconPropTypes';

const CreateIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-create" size={size} color={color} />
);

CreateIcon.propTypes = iconPropTypes;

export default CreateIcon;
