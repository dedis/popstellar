import { BottomTabNavigationOptions } from '@react-navigation/bottom-tabs';
import { StackNavigationOptions } from '@react-navigation/stack';

import BackButton from 'core/components/BackButton';
import { Color, Spacing, Typography } from 'core/styles';

export const stackScreenOptionsWithoutHeader: StackNavigationOptions = {
  headerShown: false,
  headerLeft: BackButton,
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
  // Since we explicitly use scroll views for screens, we should disable scrolling for
  // the navigator so that the header sticks to the top of the screen
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const tabNavigationOptions: BottomTabNavigationOptions = {
  tabBarActiveTintColor: Color.accent,
  tabBarInactiveTintColor: Color.inactive,
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
