import { TextStyle } from 'react-native';

export const base: TextStyle = {
  textAlign: 'left',
  fontSize: 25,
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
