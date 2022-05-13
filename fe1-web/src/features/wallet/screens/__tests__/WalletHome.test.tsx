import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockLaoIdHash, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { getEventById } from 'features/events/functions';
import { addEvent, eventsReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { RollCallHooks } from 'features/rollCall/hooks';
import { RollCall, RollCallStatus } from 'features/rollCall/objects';
import { addRollCall } from 'features/rollCall/reducer';
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

const mockRCName = 'myRollCall';
const mockRCLocation = 'location';
const mockRCTimestampStart = new Timestamp(1620255600);
const mockRCTimestampEnd = new Timestamp(1620357600);
const mockRCAttendees = ['attendee1', 'attendee2'];

const mockRCIdAliasHash = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLaoId,
  mockRCTimestampStart.toString(),
  mockRCName,
);

const mockRCIdHash = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLaoId,
  mockRCIdAliasHash.valueOf(),
  mockRCName,
);

const mockRollCallState = {
  id: mockRCIdHash.valueOf(),
  idAlias: mockRCIdAliasHash.valueOf(),
  eventType: RollCall.EVENT_TYPE,
  start: mockRCTimestampStart.valueOf(),
  end: mockRCTimestampEnd.valueOf(),
  name: mockRCName,
  location: mockRCLocation,
  creation: mockRCTimestampStart.valueOf(),
  proposedStart: mockRCTimestampStart.valueOf(),
  proposedEnd: mockRCTimestampEnd.valueOf(),
  status: RollCallStatus.CLOSED,
  attendees: mockRCAttendees,
};
const mockRollCall = RollCall.fromState(mockRollCallState);

const contextValue = {
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    getEventById,
    useRollCallsByLaoId: RollCallHooks.useRollCallsByLaoId,
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
      rollCallId: mockRollCall.id,
      rollCallName: mockRollCall.name,
    });

    // make the selector return data
    mockStore.dispatch(
      addEvent(mockLaoId, RollCall.EVENT_TYPE, mockRollCall.id, mockRollCall.idAlias),
    );
    mockStore.dispatch(addRollCall(mockRollCallState));

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
      makeEventByTypeSelector(RollCall.EVENT_TYPE)(mockStore.getState()),
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
