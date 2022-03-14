import React from 'react';

export namespace LaoFeature {
  export interface LaoScreen {
    name: string;
    component: React.ComponentType<unknown>;
    order: number;
  }

  export interface LaoConnection {
    server: string;
    lao: string;
  }
}
