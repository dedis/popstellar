import { TextStyle, ViewStyle } from 'react-native';

import { radius } from './border';
import { contrast } from './color';
import { x2 } from './spacing';
import { topNavigationHeading } from './typography';

export const modalTitle: TextStyle = topNavigationHeading;

export const modalBackground: ViewStyle = {
  position: 'absolute',
  left: 0,
  right: 0,
  top: 0,
  bottom: 0,
  padding: x2,
  backgroundColor: 'rgba(0,0,0,0.25)',
  flex: 1,
};
export const modalContainer: ViewStyle = {
  margin: x2,
  padding: x2,
  backgroundColor: contrast,
  borderRadius: radius,
};
