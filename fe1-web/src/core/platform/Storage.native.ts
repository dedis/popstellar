import * as SecureStore from 'expo-secure-store';

type Key = string | number;
type Updater<T> = (oldValue: T | undefined) => T;

export async function get(key: Key): Promise<any> {
  console.log('get', SecureStore.getItemAsync(key.toString()));
  return SecureStore.getItemAsync(key.toString());
}

export async function set(key: Key, value: any): Promise<void> {
  console.log('set', value);
  await SecureStore.setItemAsync(key.toString(), value);
}

export async function update<T>(key: Key, updater: Updater<T>): Promise<void> {
  const oldValue = await get(key);
  console.log('update', oldValue, 'to', updater(oldValue));
  set(key, updater(oldValue));
}
