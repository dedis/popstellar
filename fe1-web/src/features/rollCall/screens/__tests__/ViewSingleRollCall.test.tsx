import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoIdHash, mockLao, mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash, Timestamp } from 'core/objects';
import { addEvent, eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { connectToLao, laoReducer } from 'features/lao/reducer';
import { mockRollCall } from 'features/rollCall/__tests__/utils';
import { RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from 'features/rollCall/interface';
import { addRollCall, rollCallReducer, updateRollCall } from 'features/rollCall/reducer';
import { generateToken } from 'features/wallet/objects';
import { getWalletState, walletReducer } from 'features/wallet/reducer';

import { RollCall, RollCallStatus } from '../../objects';
import ViewSingleRollCall, { ViewSinglRollCallScreenRightHeader } from '../ViewSingleRollCall';

const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620255600);
const TIMESTAMP_END = new Timestamp(1620357600);
const ATTENDEES = ['attendee1', 'attendee2'];

const createStateWithStatus: any = (mockStatus: RollCallStatus) => {
  return {
    id: ID.valueOf(),
    eventType: RollCall.EVENT_TYPE,
    start: TIMESTAMP_START.valueOf(),
    name: NAME,
    location: LOCATION,
    creation: TIMESTAMP_START.valueOf(),
    proposedStart: TIMESTAMP_START.valueOf(),
    proposedEnd: TIMESTAMP_END.valueOf(),
    status: mockStatus,
    attendees: ATTENDEES,
    idAlias: mockStatus === RollCallStatus.CREATED ? undefined : ID.valueOf(),
  };
};

const mockRollCallCreated = RollCall.fromState(createStateWithStatus(RollCallStatus.CREATED));

// set up mock store
const mockStore = createStore(
  combineReducers({ ...laoReducer, ...eventReducer, ...rollCallReducer, ...walletReducer }),
);
mockStore.dispatch(connectToLao(mockLao.toState()));
const mockRollCallState = mockRollCallCreated.toState();

mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: RollCall.EVENT_TYPE,
    id: mockRollCallState.id,
    start: mockRollCall.start.valueOf(),
    end: mockRollCall.end.valueOf(),
  }),
);
mockStore.dispatch(addRollCall(mockRollCallState));

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    makeEventByTypeSelector,
    generateToken,
    hasSeed: () => getWalletState(mockStore.getState()).seed !== undefined,
  } as RollCallReactContext,
};

jest.spyOn(Date.prototype, 'toLocaleDateString').mockReturnValue('2022-05-28');
jest.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('00:00:00');

beforeEach(() => {
  jest.clearAllMocks();
});

describe('EventRollCall', () => {
  it('should correctly render', () => {
    mockStore.dispatch(updateRollCall(mockRollCallCreated.toState()));

    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSingleRollCall}
            params={{ eventId: mockRollCallCreated.id.valueOf(), isOrganizer: false }}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    expect(obj.toJSON()).toMatchSnapshot();
  });
});

describe('ViewSinglRollCallScreenRightHeader', () => {
  it('should correctly render', () => {
    mockStore.dispatch(updateRollCall(mockRollCallCreated.toState()));

    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSinglRollCallScreenRightHeader}
            params={{ eventId: mockRollCallCreated.id.valueOf(), isOrganizer: true }}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    expect(obj.toJSON()).toMatchSnapshot();
  });
});
