export namespace HomeFeature {
  export interface LaoState {}

  export interface Lao {
    toState: () => LaoState;
  }

  export interface Screen {
    id: string;
    title?: string;
    Component: React.ComponentType<unknown>;
    order: number;
  }
}
