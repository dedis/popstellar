import { fireEvent, render } from '@testing-library/react-native';
import * as Clipboard from 'expo-clipboard';
import React from 'react';

import CopyButton from '../CopyButton';

jest.mock('expo-clipboard');

describe('CopyButton', () => {
  it('renders correctly', () => {
    const { toJSON } = render(<CopyButton data="content" />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when negative', () => {
    const { toJSON } = render(<CopyButton data="content" negative />);
    expect(toJSON()).toMatchSnapshot();
  });

  it('copies to clipboard correctly on click', () => {
    const { getByTestId } = render(<CopyButton data="data" />);
    fireEvent.press(getByTestId('copyButton'));
    expect(Clipboard.setStringAsync).toHaveBeenCalledWith('data');
  });
});
