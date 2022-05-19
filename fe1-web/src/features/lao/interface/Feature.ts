import React, { ReactNode } from 'react';

import { LaoOrganizerParamList } from 'core/navigation/typing/LaoOrganizerParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';

export namespace LaoFeature {
  export type OrganizerScreen = Omit<LaoScreen, 'id'> & { id: keyof LaoOrganizerParamList };

  export interface LaoScreen {
    id: keyof LaoParamList;
    title?: string;
    Component: React.ComponentType<unknown>;

    /**
     * This number is here to order the screens.
     * The numbers have to be unique, otherwise an error will be thrown.
     * In order to be able to insert screens in between two existing screens,
     * do *not* use numbers 1,2,3,... but rather ones with big gaps in between,
     * e.g. -9999999999, -1000, -10, 0, 100, ... etc.
     */
    order: number;

    Badge?: () => ReactNode;
    Icon?: () => ReactNode;
  }

  export interface LaoConnection {
    server: string;
    lao: string;
  }
}
