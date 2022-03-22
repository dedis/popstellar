import React from 'react';

export namespace LaoFeature {
  export interface Screen {
    id: string;
    title?: string;
    Component: React.ComponentType<unknown>;
    order: number;
  }

  export interface LaoConnection {
    server: string;
    lao: string;
  }
}
