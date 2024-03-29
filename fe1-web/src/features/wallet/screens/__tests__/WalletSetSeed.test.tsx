import { render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';

import { WalletSetSeed } from '../index';

beforeEach(() => {
  jest.clearAllMocks();
});
describe('Wallet set seed screen', () => {
  it('renders correctly', () => {
    const component = render(<MockNavigator component={WalletSetSeed} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
