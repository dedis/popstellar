import { TextStyle } from 'react-native';

import * as Spacing from './spacing';

export const base = {
  textAlign: 'left',
  fontSize: 25,
  marginHorizontal: Spacing.xs,
} as TextStyle;

export const important = {
  ...base,
  fontWeight: 'bold',
} as TextStyle;

export const baseCentered = {
  ...base,
  textAlign: 'center',
} as TextStyle;

export const importantCentered = {
  ...baseCentered,
  fontWeight: 'bold',
} as TextStyle;
