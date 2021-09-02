const storage = new Map();

type Key = string | number;
type Updater<T> = (oldValue: T | undefined) => T;

export function get(key: Key): any {
  return storage.get(key);
}

export function set(key: Key, value: any) {
  storage.set(key, value);
}

export function update<T>(key: Key, updater: Updater<T>) {
  const newValue = updater(get(key));
  set(key, newValue);
}
