import 'react-native-gesture-handler/jestSetup';

import { jest } from '@jest/globals';

// Silence the warning: Animated: `useNativeDriver` is not supported
// because the native animated module is missing
jest.mock('react-native/Libraries/Animated/NativeAnimatedHelper');

// Icons and snapshot tests do not work nice together
// See https://github.com/expo/expo/issues/3566
jest.mock('@expo/vector-icons');
jest.mock('react-blockies', () => 'blockies');

// qr codes are svgs and thus generate huge snapshot files
jest.mock('core/components/QRCode.tsx', () => 'qrcode');

// make functions return a value independent of the CI locale
jest.spyOn(Date.prototype, 'toLocaleDateString').mockReturnValue('2022-05-28');
jest.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('00:00:00');

// Fixes issue with react-native-reanimated
// https://github.com/software-mansion/react-native-reanimated/issues/3125#issuecomment-1085737354
global.ReanimatedDataMock = {
  now: () => 0,
};
