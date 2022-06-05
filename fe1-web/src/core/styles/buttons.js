import * as Colors from './colors';
import * as Spacing from './spacing';

export const base = {
  marginHorizontal: Spacing.xl,
  marginVertical: Spacing.xs,
};

export const baseBold = {
  ...base,
  fontWeight: 'bold',
};

export const defaultButtonIconSize = 25;
export const defaultButtonIconFamily = 'AntDesign';
export const defaultButtonIconColor = Colors.white;
