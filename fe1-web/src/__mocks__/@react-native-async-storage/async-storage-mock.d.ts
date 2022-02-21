// We need this file because the async storage has not yet been mocked.
// cf. https://github.com/react-native-async-storage/async-storage/issues/504

declare module '@react-native-async-storage/async-storage/jest/async-storage-mock' {
  import type { AsyncStorageStatic } from '@react-native-async-storage/async-storage/types';

  const AsyncStorageMock = AsyncStorageStatic;
  export default AsyncStorageMock;
}
