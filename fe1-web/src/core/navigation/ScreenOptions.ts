import { DrawerNavigationOptions } from '@react-navigation/drawer';
import { StackNavigationOptions } from '@react-navigation/stack';

import BackButton from 'core/components/BackButton';
import NavigationPadding from 'core/components/NavigationPadding';
import { Color, Spacing, Typography } from 'core/styles';

export const stackScreenOptionsWithoutHeader: StackNavigationOptions = {
  headerShown: false,
  headerLeft: BackButton,
  headerRight: NavigationPadding,
  // Since we explicitly use scroll views for screens, we should disable scrolling for
  // the navigator so that the header sticks to the top of the screen
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const stackScreenOptionsWithHeader: StackNavigationOptions = {
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
  headerLeft: BackButton,
  headerRight: NavigationPadding,
  // Since we explicitly use scroll views for screens, we should disable scrolling for
  // the navigator so that the header sticks to the top of the screen
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const drawerNavigationOptions: DrawerNavigationOptions = {
  drawerActiveBackgroundColor: Color.accentLight,
  drawerActiveTintColor: Color.accent,
  drawerInactiveTintColor: Color.inactive,

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
