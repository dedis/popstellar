import Ionicons from '@expo/vector-icons/Ionicons';
import React from 'react';

import { iconDefaultProps, iconPropTypes, IconPropTypes } from './IconPropTypes';

const QrCodeIcon = ({ color, size }: IconPropTypes) => (
  <Ionicons name="ios-qr-code" size={size} color={color} />
);

QrCodeIcon.propTypes = iconPropTypes;
QrCodeIcon.defaultProps = iconDefaultProps;

export default QrCodeIcon;
