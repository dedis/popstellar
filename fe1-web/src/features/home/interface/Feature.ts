export namespace HomeFeature {
  export interface LaoState {}

  export interface Lao {
    toState: () => LaoState;
  }

  export interface MainNavigationScreen {
    name: string;
    Component: React.ComponentType<unknown>;
    order: number;
  }
}
