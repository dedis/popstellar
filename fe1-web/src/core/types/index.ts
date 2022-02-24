// Copied from https://stackoverflow.com/a/49062616/2897827
export type NonMethodKeys<T> = ({ [P in keyof T]: T[P] extends Function ? never : P } & {
  [x: string]: never;
})[keyof T];

// Copied from https://stackoverflow.com/a/49062616/2897827
export type RemoveMethods<T> = Pick<T, NonMethodKeys<T>>;

export type MessageDataProperties<T> = Omit<RemoveMethods<T>, 'object' | 'action'>;
