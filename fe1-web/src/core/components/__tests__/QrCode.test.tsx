import { render, waitFor } from '@testing-library/react-native';
import React from 'react';

import QRCode from '../QRCode';

const shortText = '8eFmp65HHy7HjJjfTVqfx9t6x5xNqG6MaqRJ2xQaSPvs5vSj5j5RQjU5aS';
// The type is the one of the warn function
let tmpConsoleWarn: {
  (...data: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
  (...data: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
};
beforeAll(() => {
  tmpConsoleWarn = global.console.warn;
  global.console.warn = jest.fn();
});
describe('QrCode throws ', () => {
  // It is useless to test the contrary since we would wait and validate instantly even it would be wrong
  it('a warning, short text with overlay', () => {
    render(<QRCode value={shortText} overlayText="Overlay text" />);
    waitFor(() => expect(global.console.warn).toHaveBeenCalled());
  });
});

afterAll(() => {
  global.console.warn = tmpConsoleWarn;
});
