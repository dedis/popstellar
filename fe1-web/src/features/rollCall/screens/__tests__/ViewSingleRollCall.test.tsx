import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash, Timestamp } from 'core/objects';
import { addEvent, eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import { mockRollCall } from 'features/rollCall/__tests__/utils';
import { ROLLCALL_FEATURE_IDENTIFIER, RollCallReactContext } from 'features/rollCall/interface';
import { addRollCall, rollCallReducer, updateRollCall } from 'features/rollCall/reducer';
import { generateToken } from 'features/wallet/objects';
import { getWalletState, walletReducer } from 'features/wallet/reducer';

import { RollCall, RollCallStatus } from '../../objects';
import ViewSingleRollCall from '../ViewSingleRollCall';

const ID = new Hash('rollCallId');
const NAME = 'myRollCall';
const LOCATION = 'location';
const TIMESTAMP_START = new Timestamp(1620355600);
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
    openedAt: TIMESTAMP_START.valueOf(),
    closedAt: TIMESTAMP_END.valueOf(),
    status: mockStatus,
    attendees: ATTENDEES,
    idAlias: mockStatus === RollCallStatus.CREATED ? undefined : ID.valueOf(),
  };
};

const mockRollCallCreated = RollCall.fromState(createStateWithStatus(RollCallStatus.CREATED));
const mockRollCallOpened = RollCall.fromState(createStateWithStatus(RollCallStatus.OPENED));
const mockRollCallReopened = RollCall.fromState(createStateWithStatus(RollCallStatus.REOPENED));
const mockRollCallClosed = RollCall.fromState(createStateWithStatus(RollCallStatus.CLOSED));

// set up mock store
const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...eventReducer,
    ...rollCallReducer,
    ...walletReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));
const mockRollCallState = mockRollCallCreated.toState();

mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: RollCall.EVENT_TYPE,
    id: mockRollCallState.id,
    start: mockRollCall.start.valueOf(),
    end: mockRollCall.end?.valueOf(),
  }),
);
mockStore.dispatch(addRollCall(mockRollCallState));

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    makeEventByTypeSelector,
    generateToken,
    hasSeed: () => getWalletState(mockStore.getState()).seed !== undefined,
  } as RollCallReactContext,
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('EventRollCall', () => {
  describe('render correctly', () => {
    const testRender = (rollCall: RollCall, isOrganizer: boolean) => () => {
      mockStore.dispatch(updateRollCall(rollCall.toState()));

      const obj = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <MockNavigator
              component={ViewSingleRollCall}
              params={{
                eventId: rollCall.id.valueOf(),
                isOrganizer,
              }}
            />
          </FeatureContext.Provider>
        </Provider>,
      );

      expect(obj.toJSON()).toMatchSnapshot();
    };

    describe('organizers', () => {
      it('created roll calls', testRender(mockRollCallCreated, true));
      it('opened roll calls', testRender(mockRollCallOpened, true));
      it('re-opened roll calls', testRender(mockRollCallReopened, true));
      it('closed roll calls', testRender(mockRollCallClosed, true));
    });

    describe('non organizers', () => {
      it('created roll calls', testRender(mockRollCallCreated, false));
      it('opened roll calls', testRender(mockRollCallOpened, false));
      it('re-opened roll calls', testRender(mockRollCallReopened, false));
      it('closed roll calls', testRender(mockRollCallClosed, false));
    });
  });
});
