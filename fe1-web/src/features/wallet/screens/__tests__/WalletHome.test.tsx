import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockLaoIdHash, mockLaoName, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { getEventById } from 'features/events/functions';
import { addEvent, eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { RollCallHooks } from 'features/rollCall/hooks';
import { RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from 'features/rollCall/interface';
import { RollCall, RollCallStatus } from 'features/rollCall/objects';
import { addRollCall, rollCallReducer } from 'features/rollCall/reducer';
import { hasSeed } from 'features/wallet/functions';
import { WalletReactContext, WALLET_FEATURE_IDENTIFIER } from 'features/wallet/interface';
import { walletReducer } from 'features/wallet/reducer';

import { generateToken, recoverWalletRollCallTokens } from '../../objects';
import { RollCallToken } from '../../objects/RollCallToken';
import { WalletHome } from '../index';

jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');
jest.mock('features/wallet/objects/Wallet');
jest.mock('core/components/QRCode.tsx', () => 'qrcode');

// disable animations
jest.useFakeTimers('modern');
jest.setSystemTime(new Date(1620255600000)); // 5 May 2021

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
    useLaoIds: () => [mockLaoIdHash],
    useNamesByLaoId: () => ({ [mockLaoId]: mockLaoName }),
    walletItemGenerators: [],
    walletNavigationScreens: [],
  } as WalletReactContext,
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    generateToken,
    hasSeed,
    makeEventByTypeSelector,
  } as RollCallReactContext,
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Wallet home', () => {
  it('renders correctly with an empty wallet', () => {
    const mockStore = createStore(
      combineReducers({ ...walletReducer, ...rollCallReducer, ...eventReducer }),
    );

    (recoverWalletRollCallTokens as jest.Mock).mockImplementation(() => Promise.resolve([]));

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly with a non empty wallet', () => {
    const mockStore = createStore(
      combineReducers({ ...walletReducer, ...rollCallReducer, ...eventReducer }),
    );

    const mockRCToken = new RollCallToken({
      token: mockPopToken,
      laoId: mockLao.id,
      rollCallId: mockRollCall.id,
      rollCallName: mockRollCall.name,
    });

    // make the selector return data
    mockStore.dispatch(
      addEvent(mockLaoId, {
        eventType: RollCall.EVENT_TYPE,
        id: mockRollCallState.id,
        start: mockRollCallState.proposedStart,
        end: mockRollCallState.proposedEnd,
      }),
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

    expect(component).toMatchSnapshot();
  });
});
