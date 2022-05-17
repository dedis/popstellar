import { TextStyle } from 'react-native';

import { x05, x1 } from './spacing';

export const base: TextStyle = {
  textAlign: 'left',
  fontSize: 16,
};

export const paragraph: TextStyle = {
  ...base,
  marginBottom: x05,
};

export const heading: TextStyle = {
  ...base,
  fontSize: 24,
  fontWeight: 'bold',
  marginBottom: x1,
};

export const important: TextStyle = {
  ...base,
  fontWeight: 'bold',
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
