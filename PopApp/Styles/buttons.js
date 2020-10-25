import * as Spacing from './spacing';

// eslint-disable-next-line import/prefer-default-export
export const base = {
  marginHorizontal: Spacing.xl,
  marginVertical: Spacing.xs,
};

export const baseBold = {
  ...base,
  fontWeight: 'bold',
};
