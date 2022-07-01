import { BottomTabNavigationEventMap } from '@react-navigation/bottom-tabs';
import { EventListenerCallback } from '@react-navigation/core';

export interface NavigationScreen {
  /**
   * The unique identifier of the screen. Can be used in .navigate() and .push()
   */
  id: string;

  /**
   * The title string shown in the navigation bar (if there is any) and as the window title in the browser
   */
  title?: string;

  /**
   * The component shown in the middle of the header. Receives the title as a child and defaults to the title.
   */
  headerTitle?: string | ((props: { children: string }) => React.ReactNode);

  /**
   * The screen component
   */
  Component: React.ComponentType<unknown>;

  /**
   * Whether to show the header
   */
  headerShown?: boolean;

  /**
   * A component to render in the right corner of the header bar (e.g. three dots)
   */
  headerRight?: HeaderComponent;

  /**
   * A component to render in the left corner of the header bar (e.g. a burger menu)
   */
  headerLeft?: HeaderComponent;
}

export interface NavigationTabScreen extends NavigationScreen {
  /**
   * Set to null if no icon should be shown
   */
  tabBarIcon?:
    | null
    | ((props: { focused: boolean; color: string; size: number }) => React.ReactNode);

  /**
   * Callback when the tab bar element is pressed
   */
  tabPress?: EventListenerCallback<BottomTabNavigationEventMap, 'tabPress'>;

  /**
   * This number is here to order the screens.
   * The numbers have to be unique, otherwise an error will be thrown.
   * In order to be able to insert screens in between two existing screens,
   * do *not* use numbers 1,2,3,... but rather ones with big gaps in between,
   * e.g. -9999999999, -1000, -10, 0, 100, ... etc.
   */
  order: number;

  /**
   * The test id that is set for the navigation bar element
   */
  testID?: string;
}

type HeaderComponent =
  | ((props: {
      tintColor?: string;
      pressColor?: string;
      pressOpacity?: number;
      labelVisible?: boolean;
    }) => React.ReactNode)
  | undefined;
