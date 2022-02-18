import * as Spacing from './spacing';

export const base = {
  textAlign: 'center',
  fontSize: 25,
  marginHorizontal: Spacing.xs,
};

export const important = {
  ...base,
  fontWeight: 'bold',
};
