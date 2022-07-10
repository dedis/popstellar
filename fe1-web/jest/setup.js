import 'react-native-gesture-handler/jestSetup';

import { jest } from '@jest/globals';

jest.mock('react-native-reanimated', () => {
  // eslint-disable-next-line global-require
  const Reanimated = require('react-native-reanimated/mock');

  // The mock for `call` immediately calls the callback which is incorrect
  // So we override it with a no-op
  Reanimated.default.call = () => {
    /* no-op */
  };

  return Reanimated;
});

// Silence the warning: Animated: `useNativeDriver` is not supported
// because the native animated module is missing
jest.mock('react-native/Libraries/Animated/NativeAnimatedHelper');

jest.mock('react-blockies', () => 'blockies');
