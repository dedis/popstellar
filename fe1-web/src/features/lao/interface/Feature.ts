import React from 'react';

export namespace LaoFeature {
  export interface Screen {
    name: string;
    Component: React.ComponentType<unknown>;
    order: number;
  }

  export interface LaoConnection {
    server: string;
    lao: string;
  }
}
