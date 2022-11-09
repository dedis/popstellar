import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLaoId, mockLaoIdHash, mockLaoName } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EventTags, Hash, PopToken, RollCallToken, Timestamp } from 'core/objects';
import { getEventById } from 'features/events/functions';
import { addEvent, eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { RollCallHooks } from 'features/rollCall/hooks';
import { ROLLCALL_FEATURE_IDENTIFIER, RollCallReactContext } from 'features/rollCall/interface';
import { RollCall, RollCallStatus } from 'features/rollCall/objects';
import { addRollCall, rollCallReducer } from 'features/rollCall/reducer';
import { hasSeed } from 'features/wallet/functions';
import { WALLET_FEATURE_IDENTIFIER, WalletReactContext } from 'features/wallet/interface';
import { walletReducer } from 'features/wallet/reducer';

import { generateToken } from '../../objects';
import { WalletHome } from '../index';

jest.mock('core/platform/Storage');
jest.mock('core/platform/crypto/browser');

const mockRCName = 'myRollCall';
const mockRCLocation = 'location';
const mockRCTimestampStart = new Timestamp(1620355600);
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
const mockRollCallToken: RollCallToken = {
  laoId: mockLaoIdHash,
  rollCallId: mockRollCall.id,
  rollCallName: mockRollCall.name,
  token: PopToken.fromState(mockKeyPair.toState()),
};

const contextValue = (rollCallTokens: RollCallToken[]) => ({
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    getEventById,
    useRollCallsByLaoId: RollCallHooks.useRollCallsByLaoId,
    useRollCallTokensByLaoId: () => rollCallTokens,
    useLaoIds: () => [mockLaoIdHash],
    useNamesByLaoId: () => ({ [mockLaoId]: mockLaoName }),
    walletItemGenerators: [],
    walletNavigationScreens: [],
  } as WalletReactContext,
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useAssertCurrentLaoId: () => mockLaoIdHash,
    generateToken,
    hasSeed,
    makeEventByTypeSelector,
  } as RollCallReactContext,
});

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Wallet home', () => {
  it('renders correctly with an empty wallet', () => {
    const mockStore = configureStore({
      reducer: combineReducers({
        ...walletReducer,
        ...rollCallReducer,
        ...eventReducer,
      }),
    });

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue([])}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly with a non empty wallet', () => {
    const mockStore = configureStore({
      reducer: combineReducers({
        ...walletReducer,
        ...rollCallReducer,
        ...eventReducer,
      }),
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

    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue([mockRollCallToken])}>
          <MockNavigator component={WalletHome} />
        </FeatureContext.Provider>
      </Provider>,
    );

    expect(component).toMatchSnapshot();
  });
});
