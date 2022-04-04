import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { useSelector } from 'react-redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockPopToken, mockRC } from '__tests__/utils';

import { recoverWalletRollCallTokens } from '../../objects';
import { clearMockWalletState, createMockWalletState } from '../../objects/__mocks__/mockWallet';
import { RollCallToken } from '../../objects/RollCallToken';
import { WalletHome } from '../index';

jest.mock('react-redux');
jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');
jest.mock('features/wallet/objects/Wallet');
jest.mock('features/wallet/objects/__mocks__/mockWallet');

beforeEach(() => {
  jest.clearAllMocks();
});
describe('Wallet home', () => {
  it('renders correctly with an empty wallet', () => {
    const component = render(<MockNavigator component={WalletHome} />).toJSON();
    expect(component).toMatchSnapshot();
  });
  it('renders correctly with a non empty wallet', async () => {
    const mockRCToken = new RollCallToken({
      token: mockPopToken,
      laoId: mockLao.id,
      rollCallId: mockRC.id,
      rollCallName: mockRC.name,
    });

    // Just for the selector object to not be null
    const mockRecord = { mock: { mock: 'deepMock' } };
    (useSelector as jest.Mock)
      .mockImplementationOnce(() => mockRecord)
      .mockImplementationOnce(() => mockLao);

    (recoverWalletRollCallTokens as jest.Mock).mockImplementation(
      () => new Promise<RollCallToken[]>(() => [mockRCToken]),
    );

    render(<MockNavigator component={WalletHome} />);
  });
  it('enables correctly the debug mode', async () => {
    const mockCreateWalletState = (createMockWalletState as jest.Mock).mockImplementation(
      () => new Promise(() => undefined),
    );
    const { getByText } = render(<MockNavigator component={WalletHome} />);

    const toggleDebugButton = getByText('Set debug mode on [TESTING]');

    fireEvent.press(toggleDebugButton);
    await waitFor(() => {
      expect(mockCreateWalletState).toHaveBeenCalledTimes(1);
    });
  });
  it('disables correctly the debug mode', async () => {
    (createMockWalletState as jest.Mock).mockImplementation(
      () => new Promise(() => undefined),
    );

    const mockClearWalletState = clearMockWalletState as jest.Mock;
    const { getByText } = render(<MockNavigator component={WalletHome} />);

    const toggleButton = getByText('Set debug mode on [TESTING]');
    fireEvent.press(toggleButton);

    await waitFor(() => {
      fireEvent.press(toggleButton);
      expect(mockClearWalletState).toHaveBeenCalledTimes(1);
    });
  });
});
