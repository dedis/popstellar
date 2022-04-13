import { expect } from '@jest/globals';
import { render, waitFor } from '@testing-library/react-native';
import React from 'react';

import STRINGS from 'resources/strings';

import { WalletNavigation } from '../index';

const mockHasSeed = jest.fn();

jest.mock('../../store', () => {
  return {
    WalletStore: {
      hasSeed: () => mockHasSeed,
    },
  };
});
jest.mock('react-navigation');

beforeEach(() => {
  jest.clearAllMocks();
});
describe('Wallet navigation', () => {
  it('renders correctly', () => {
    const component = render(<WalletNavigation />).toJSON();
    expect(component).toMatchSnapshot();
  });
  it('renders home screen when seed defined', async () => {
    mockHasSeed.mockReturnValue(true);
    const { getByText } = render(<WalletNavigation />);
    await waitFor(() => getByText(STRINGS.navigation_home_tab_wallet));
  });
  it('renders home screen when no seed defined', async () => {
    mockHasSeed.mockReturnValue(false);
    const { getByText } = render(<WalletNavigation />);
    await waitFor(() => getByText(STRINGS.navigation_synced_wallet));
  });
});
