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

// Icons and snapshot tests do not work nice together
// See https://github.com/expo/expo/issues/3566
jest.mock('@expo/vector-icons');

// qr codes are svgs and thus generate huge snapshot files
jest.mock('core/components/QRCode.tsx', () => 'qrcode');

// set a fixed system time to avoid issues with relative time computations
const FIXED_SYSTEM_TIME = new Date(1620255600 * 1000); // 5 May 2021
jest.useFakeTimers('modern');
jest.setSystemTime(FIXED_SYSTEM_TIME);

// make functions return a value independent of the CI locale
jest.spyOn(Date.prototype, 'toLocaleDateString').mockReturnValue('2022-05-28');
jest.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('00:00:00');
