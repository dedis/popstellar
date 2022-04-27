import { expect } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';

import { WalletError } from '../index';

const mockSeed = 'one two three';

jest.mock('features/wallet/objects/Seed', () => {
  return {
    generateMnemonicSeed: () => mockSeed,
  };
});
beforeEach(() => {
  jest.clearAllMocks();
});
describe('Wallet error screen', () => {
  it('renders correctly', () => {
    const component = render(<MockNavigator component={WalletError} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
