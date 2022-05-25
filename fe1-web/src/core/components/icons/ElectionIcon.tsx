import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const ElectionIcon = ({ color, size }: IconPropTypes) => (
  <MaterialIcons name="how-to-vote" size={size} color={color} />
);

ElectionIcon.propTypes = iconPropTypes;
ElectionIcon.defaultProps = iconDefaultProps;

export default ElectionIcon;
