import { BottomTabNavigationEventMap } from '@react-navigation/bottom-tabs';
import { EventListenerCallback } from '@react-navigation/core';

import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';

export namespace HomeFeature {
  export interface LaoState {
    id: string;
  }

  export interface Lao {
    toState: () => LaoState;
  }

  export type LaoScreen = Omit<HomeScreen, 'id'> & { id: keyof LaoParamList };

  export interface HomeScreen {
    id: keyof HomeParamList;
    title?: string;

    Component: React.ComponentType<unknown>;

    tabBarIcon?: (props: { focused: boolean; color: string; size: number }) => React.ReactNode;

    tabPress?: EventListenerCallback<BottomTabNavigationEventMap, 'tabPress'>;

    headerLeft?: HeaderComponent;
    headerRight?: HeaderComponent;

    /**
     * This number is here to order the screens.
     * The numbers have to be unique, otherwise an error will be thrown.
     * In order to be able to insert screens in between two existing screens,
     * do *not* use numbers 1,2,3,... but rather ones with big gaps in between,
     * e.g. -9999999999, -1000, -10, 0, 100, ... etc.
     */
    order: number;
  }

  type HeaderComponent =
    | ((props: {
        tintColor?: string | undefined;
        pressColor?: string | undefined;
        pressOpacity?: number | undefined;
        labelVisible?: boolean | undefined;
      }) => React.ReactNode)
    | undefined;
}
