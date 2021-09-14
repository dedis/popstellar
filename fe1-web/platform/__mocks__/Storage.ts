const storage = new Map();

type Key = string | number;
type Updater<T> = (oldValue: T | undefined) => T;

export function get(key: Key): Promise<any> {
  return Promise.resolve(storage.get(key));
}

export function set(key: Key, value: any): Promise<void> {
  storage.set(key, value);
  return Promise.resolve();
}

export async function update<T>(key: Key, updater: Updater<T>): Promise<void> {
  const value = await get(key);
  const newValue = updater(value);
  return set(key, newValue);
}

export function getMockStorage(): Map<any, any> {
  return storage;
}
