import React from 'react';

export namespace LaoFeature {
  export interface Screen {
    id: string;
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
  }

  export interface LaoConnection {
    server: string;
    lao: string;
  }
}
