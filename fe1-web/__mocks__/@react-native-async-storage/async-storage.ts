// <reference path="async-storage-mock.d.ts">
import asyncMock from '@react-native-async-storage/async-storage/jest/async-storage-mock';

const { setItem } = asyncMock;

const fixedMock = {
  ...asyncMock,

  /**
   * This is our custom variation on the async mock: we need to ensure that a Promise is returned.
   * Otherwise, tests fail due to a missing .catch() method on the returned object.
   *
   * @param key
   * @param value
   */
  setItem: (key: string, value: string): Promise<void> => {
    setItem(key, value);
    return Promise.resolve();
  },
};

export default fixedMock;
