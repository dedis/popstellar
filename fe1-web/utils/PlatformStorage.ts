import {
  get as kvGet,
  set as kvSet,
  update as kvUpdate,
} from 'idb-keyval';

type Key = string | number;
type Updater<T> = (oldValue: T | undefined) => T;

export function get(key: Key): any {
  return kvGet(key);
}

export function set(key: Key, value: any) {
  kvSet(key, value);
}

export function update<T>(key: Key, updater: Updater<T>) {
  kvUpdate(key, updater);
}
