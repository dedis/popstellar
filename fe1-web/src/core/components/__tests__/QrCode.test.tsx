import { render, waitFor } from '@testing-library/react-native';
import React from 'react';

import QRCode from '../QRCode';

const shortText = '8eFmp65HHy7HjJjfTVqfx9t6x5xNqG6MaqRJ2xQaSPvs5vSj5j5RQjU5aS';
const longText = `${shortText}x`;

// The type is the one of the warn function
let tmpConsoleWarn: {
  (...data: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
  (...data: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
  (message?: any, ...optionalParams: any[]): void;
};
describe('QrCode ', () => {
  // It is useless to test the contrary since we would wait and validate instantly even it would be wrong
  it('throws a warning, short text with overlay', () => {
    tmpConsoleWarn = global.console.warn;
    global.console.warn = jest.fn();

    render(<QRCode value={shortText} overlayText="Overlay text" />);
    waitFor(() =>
      expect(global.console.warn).toHaveBeenCalledWith(
        'An overlay text has been added on a QRCode whose represents a too short text (length < 50)',
      ),
    );

    global.console.warn = tmpConsoleWarn;
  });

  it('looks good with short text and an overlay', () => {
    const { toJSON } = render(<QRCode value={shortText} overlayText="OverlayText" />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('looks good with long text and no overlay', () => {
    const { toJSON } = render(<QRCode value={longText} />);
    expect(toJSON()).toMatchSnapshot();
  });
});
