import { render } from '@testing-library/react-native';
import React from 'react';

import QRCode from '../QRCode';

const shortText = '8eFmp65HHy7HjJjfTVqfx9t6x5xNqG6MaqRJ2xQaSPvs5vSj5j5RQjU5aS';
const longText = `${shortText}x`;
describe('QrCode ', () => {
  // It is useless to test the contrary since we would wait and validate instantly even it would be wrong
  it('throws a warning, short text with overlay', () => {
    const spy = jest.spyOn(global.console, 'warn');

    render(<QRCode value={shortText} overlayText="Overlay text" />);
    expect(spy).toHaveBeenCalledWith(
      'An overlay text has been added on a QRCode whose represents a too short text (length < 50)',
    );
  });

  it('looks good with short text and no overlay', () => {
    const { toJSON } = render(<QRCode value={shortText} />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('looks good with long text and an overlay', () => {
    const { toJSON } = render(<QRCode value={longText} overlayText="OverlayText" />);
    expect(toJSON()).toMatchSnapshot();
  });
});
