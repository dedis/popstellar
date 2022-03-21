// Copied from https://stackoverflow.com/a/49062616/2897827
/**
 * Returns a union type consisting of all non-method keys, i.e. for {'a': 0, 'b': 1, 'c': () => {}} it is 'a' | 'b'
 *
 * @privateRemarks
 *
 * Given a type T, this returns a type with all keys set
 * keyof T returns a union of the keys of T, i.e. if T = {a: number, b: string, c: () => 0}, then keyof T = 'a' | 'b' | 'c'
 * T['a' | 'b' | 'c'] returns a union of types for the given keys, i.e. using the previous example T['a' | 'b'] = number | string | () => 0
 * Combining this we have the following:
 * { [P in keyof T]: T[P] extends Function ? never : P } = {'a': 'a', 'b': 'b', 'c': never} with the previous example
 * Then selecting of this the union of keys gives 'a' | 'b' | never = 'a' | 'b'
 */
export type NonMethodKeys<T> = { [P in keyof T]: T[P] extends Function ? never : P }[keyof T];

// Copied from https://stackoverflow.com/a/49062616/2897827
/**
 * Given a type T, this picks all keys selected by NonMethodKeys<T> which are exactly
 * the keys that do not have a method as their value
 */
export type RemoveMethods<T> = Pick<T, NonMethodKeys<T>>;

export type MessageDataProperties<T> = Omit<RemoveMethods<T>, 'object' | 'action'>;
