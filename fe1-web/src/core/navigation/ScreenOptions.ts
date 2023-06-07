import { DrawerNavigationOptions } from '@react-navigation/drawer';
import { StackNavigationOptions } from '@react-navigation/stack';
import { ViewStyle } from 'react-native';

import BackButton from 'core/components/BackButton';
import buildInfo from 'core/components/BuildInfo';
import ButtonPadding from 'core/components/ButtonPadding';
import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { Color, Spacing, Typography } from 'core/styles';

export const stackScreenOptionsWithoutHeader: StackNavigationOptions = {
  headerShown: false,
  headerLeft: BackButton,
  headerRight: ButtonPadding,
  // Since we explicitly use scroll views for screens, we should disable scrolling for
  // the navigator so that the header sticks to the top of the screen
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const stackScreenOptionsWithHeader: StackNavigationOptions = {
  headerBackground: buildInfo,
  headerBackgroundContainerStyle: {
    backgroundColor: Color.contrast,
    borderColor: Color.separator,
  },
  headerLeftContainerStyle: {
    flexBasis: 'auto',
    paddingLeft: Spacing.contentSpacing,
  },
  headerRightContainerStyle: {
    flexBasis: 'auto',
    paddingRight: Spacing.contentSpacing,
  },
  headerTitleContainerStyle: {
    flexShrink: 1,
  },
  headerTitleStyle: Typography.topNavigationHeading,
  headerTitleAlign: 'center',
  headerLeft: BackButton,
  headerRight: ButtonPadding,
  // Since we explicitly use scroll views for screens, we should disable scrolling for
  // the navigator so that the header sticks to the top of the screen
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const drawerNavigationOptions: DrawerNavigationOptions = {
  drawerActiveBackgroundColor: Color.accentLight,
  drawerActiveTintColor: Color.accent,
  drawerInactiveTintColor: Color.inactive,
  drawerItemStyle: {
    marginHorizontal: 0,
  } as ViewStyle,
  headerLeft: DrawerMenuButton,
  headerLeftContainerStyle: {
    flexBasis: 'auto',
    paddingLeft: Spacing.contentSpacing,
  },
  headerRightContainerStyle: {
    flexBasis: 'auto',
    paddingRight: Spacing.contentSpacing,
  },
  headerTitleContainerStyle: {
    flexShrink: 1,
  },
  headerTitleStyle: Typography.topNavigationHeading,
  headerTitleAlign: 'center',
  headerStyle: {
    backgroundColor: Color.contrast,
    borderColor: Color.separator,
  },
};
