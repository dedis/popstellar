import { ViewStyle } from 'react-native';

import { horizontalContentSpacing, x1 } from './spacing';

export const listItem: ViewStyle = {
  paddingHorizontal: horizontalContentSpacing,
};

export const listContainer: ViewStyle = {
  marginHorizontal: -horizontalContentSpacing,
};

export const listIcon: ViewStyle = {
  marginRight: x1,
};
