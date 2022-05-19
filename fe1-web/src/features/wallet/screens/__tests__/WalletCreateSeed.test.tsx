import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { configureTestFeatures } from '__tests__/utils';

import { WalletCreateSeed } from '../index';

const mockSeed = 'one two three';

jest.mock('features/wallet/objects/Seed', () => {
  return {
    generateMnemonicSeed: () => mockSeed,
  };
});

beforeAll(() => {
  // the component checks for the existence of a seed in the global store
  configureTestFeatures();
});

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Wallet create seed screen', () => {
  it('renders correctly', () => {
    const component = render(<MockNavigator component={WalletCreateSeed} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
