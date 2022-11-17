import { ViewStyle } from 'react-native';

import { radius } from './border';
import { contrast, background } from './color';
import { contentSpacing, x1 } from './spacing';

export const item: ViewStyle = {
  backgroundColor: contrast,
};

export const accordionItem: ViewStyle = {
  backgroundColor: background,
  paddingLeft: 0,
  paddingRight: 0,
};

export const firstItem: ViewStyle = {
  borderTopLeftRadius: radius,
  borderTopRightRadius: radius,
};

export const lastItem: ViewStyle = {
  borderBottomLeftRadius: radius,
  borderBottomRightRadius: radius,
};

export const getListItemStyles = (isFirstItem: boolean, isLastItem: boolean) => {
  const listItemStyles = [item];

  if (isFirstItem) {
    listItemStyles.push(firstItem);
  }
  if (isLastItem) {
    listItemStyles.push(lastItem);
  }

  return listItemStyles;
};

/**
 * This is a workaround for https://github.com/react-native-elements/react-native-elements/issues/3200
 * To fix we need to upgrade to v4 but at the moment this breaks the displaying of all icons :(
 */
export const hiddenItem: ViewStyle = {
  display: 'none',
};

export const top: ViewStyle = {
  marginTop: -contentSpacing,
};

export const container: ViewStyle = {
  borderRadius: radius,
  marginBottom: x1,
};

export const icon: ViewStyle = {
  marginRight: x1,
};
