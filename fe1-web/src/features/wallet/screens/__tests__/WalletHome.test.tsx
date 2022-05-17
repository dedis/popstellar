import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockLaoIdHash, mockPopToken, mockRC } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { getEventById } from 'features/events/functions';
import { LaoEventType } from 'features/events/objects';
import { addEvent, eventsReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { WalletReactContext, WALLET_FEATURE_IDENTIFIER } from 'features/wallet/interface';
import { walletReducer } from 'features/wallet/reducer';
import STRINGS from 'resources/strings';

import { recoverWalletRollCallTokens } from '../../objects';
import { clearDummyWalletState, createDummyWalletState } from '../../objects/DummyWallet';
import { RollCallToken } from '../../objects/RollCallToken';
import { WalletHome } from '../index';

jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');
jest.mock('features/wallet/objects/Wallet');
jest.mock('features/wallet/objects/DummyWallet');
jest.mock('core/components/QRCode.tsx', () => 'qrcode');

const contextValue = {
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    getEventById,
    makeEventByTypeSelector,
  } as WalletReactContext,
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Wallet home', () => {
  it('renders correctly with an empty wallet', () => {
    const mockStore = createStore(combineReducers({ ...walletReducer, ...eventsReducer }));

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly with a non empty wallet', async () => {
    const mockStore = createStore(combineReducers({ ...walletReducer, ...eventsReducer }));

    const mockRCToken = new RollCallToken({
      token: mockPopToken,
      laoId: mockLao.id,
      rollCallId: mockRC.id,
      rollCallName: mockRC.name,
    });

    // make the selector return data
    mockStore.dispatch(addEvent(mockLaoId, mockRC.toState()));

    (recoverWalletRollCallTokens as jest.Mock).mockImplementation(() =>
      Promise.resolve([mockRCToken]),
    );

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    );

    expect(recoverWalletRollCallTokens).toHaveBeenCalledTimes(1);
    expect(recoverWalletRollCallTokens).toHaveBeenCalledWith(
      makeEventByTypeSelector(LaoEventType.ROLL_CALL)(mockStore.getState()),
      mockLaoIdHash,
    );

    await waitFor(() => {
      expect(() => component.getByText(STRINGS.no_tokens_in_wallet)).toThrow();
    });

    expect(component).toMatchSnapshot();
  });

  it('enables correctly the debug mode', async () => {
    const mockStore = createStore(combineReducers({ ...walletReducer, ...eventsReducer }));

    const mockCreateWalletState = (createDummyWalletState as jest.Mock).mockImplementation(() =>
      Promise.resolve(),
    );
    const { getByText } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    );
    const toggleDebugButton = getByText('Set debug mode on [TESTING]');

    fireEvent.press(toggleDebugButton);
    await waitFor(() => {
      expect(mockCreateWalletState).toHaveBeenCalledTimes(1);
    });
  });

  it('disables correctly the debug mode', async () => {
    const mockStore = createStore(combineReducers({ ...walletReducer, ...eventsReducer }));

    (createDummyWalletState as jest.Mock).mockImplementation(() => Promise.resolve());

    const mockClearWalletState = clearDummyWalletState as jest.Mock;
    const { getByText } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    );

    const toggleButtonOn = getByText('Set debug mode on [TESTING]');
    fireEvent.press(toggleButtonOn);

    await waitFor(() => {
      const toggleButtonOff = getByText('Set debug mode off [TESTING]');
      fireEvent.press(toggleButtonOff);
    });

    expect(mockClearWalletState).toHaveBeenCalledTimes(1);
  });
});
