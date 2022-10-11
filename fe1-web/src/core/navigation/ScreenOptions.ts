import { BottomTabNavigationOptions } from '@react-navigation/bottom-tabs';
import { StackNavigationOptions } from '@react-navigation/stack';

import { Color, Spacing, Typography } from 'core/styles';

export const stackScreenOptionsWithoutHeader: StackNavigationOptions = {
  headerShown: false,
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const stackScreenOptionsWithHeader: StackNavigationOptions = {
  headerLeftContainerStyle: {
    paddingLeft: Spacing.contentSpacing,
  },
  headerRightContainerStyle: {
    paddingRight: Spacing.contentSpacing,
  },
  headerTitleStyle: Typography.topNavigationHeading,
  headerTitleAlign: 'center',
  headerStyle: {
    backgroundColor: Color.contrast,
    borderColor: Color.separator,
  },
  // https://github.com/react-navigation/react-navigation/issues/9344#issuecomment-782520454
  cardStyle: { flex: 1 },
};

export const tabNavigationOptions: BottomTabNavigationOptions = {
  tabBarActiveTintColor: Color.accent,
  tabBarInactiveTintColor: Color.inactive,
  headerLeftContainerStyle: {
    paddingLeft: Spacing.contentSpacing,
  },
  headerRightContainerStyle: {
    paddingRight: Spacing.contentSpacing,
  },
  headerTitleStyle: Typography.topNavigationHeading,
  headerTitleAlign: 'center',
  headerStyle: {
    backgroundColor: Color.contrast,
    borderColor: Color.separator,
  },
};
