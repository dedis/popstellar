import { Platform, TextStyle } from 'react-native';

import {
  contrast,
  accent as accentColor,
  primary as primaryColor,
  error as errorColor,
  inactive as inactiveColor,
} from './color';
import { x1 } from './spacing';

export const base: TextStyle = {
  textAlign: 'left',
  color: primaryColor,
  fontSize: 20,
  lineHeight: 26,
};

export const small: TextStyle = {
  fontSize: 16,
  lineHeight: 20,
};

export const tiny: TextStyle = {
  fontSize: 12,
  lineHeight: 14,
};

export const accent: TextStyle = {
  color: accentColor,
};

export const error: TextStyle = {
  color: errorColor,
};

export const negative: TextStyle = {
  color: contrast,
};

export const inactive: TextStyle = {
  color: inactiveColor,
};

export const paragraph: TextStyle = {
  ...base,
  marginBottom: x1,
};

export const heading: TextStyle = {
  ...base,
  fontSize: 32,
  lineHeight: 41,
  fontWeight: 'bold',
  marginBottom: x1,
};

export const topNavigationHeading: TextStyle = {
  ...base,
  fontWeight: '500',
};

export const pressable: TextStyle = {
  ...base,
  color: accentColor,
  fontWeight: '500',
};

export const centered: TextStyle = {
  textAlign: 'center',
};

export const important: TextStyle = {
  fontWeight: 'bold',
};

// https://www.skcript.com/svr/react-native-fonts/
export const code: TextStyle = {
  ...Platform.select({
    ios: {
      fontFamily: 'Courier New',
    },
    default: {
      fontFamily: 'monospace',
    },
  }),
};

export const baseCentered: TextStyle = {
  ...base,
  textAlign: 'center',
  marginHorizontal: 10,
};

export const importantCentered: TextStyle = {
  ...baseCentered,
  fontWeight: 'bold',
};
