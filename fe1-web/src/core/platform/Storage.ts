import { get as kvGet, set as kvSet, update as kvUpdate } from 'idb-keyval';

type Key = string | number;
type Updater<T> = (oldValue: T | undefined) => T;

export async function get(key: Key): Promise<any> {
  return kvGet(key);
}

export async function set(key: Key, value: any): Promise<void> {
  await kvSet(key, value);
}

export async function update<T>(key: Key, updater: Updater<T>): Promise<void> {
  await kvUpdate(key, updater);
}
