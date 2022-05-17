import { TextStyle } from 'react-native';

export const base = {
  textAlign: 'left',
  fontSize: 25,
} as TextStyle;

export const important = {
  ...base,
  fontWeight: 'bold',
} as TextStyle;

export const baseCentered = {
  ...base,
  textAlign: 'center',
  marginHorizontal: 10,
} as TextStyle;

export const importantCentered = {
  ...baseCentered,
  fontWeight: 'bold',
} as TextStyle;
