import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const CopyIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-copy" size={size} color={color} />
);

CopyIcon.propTypes = iconPropTypes;
CopyIcon.defaultProps = iconDefaultProps;

export default CopyIcon;
