import { ViewStyle } from 'react-native';

import { contentSpacing, x1 } from './spacing';

export const item: ViewStyle = {
  paddingHorizontal: contentSpacing,
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
  marginHorizontal: -contentSpacing,
};

export const icon: ViewStyle = {
  marginRight: x1,
};

export const iconPlaceholder: ViewStyle = {
  ...icon,
};
