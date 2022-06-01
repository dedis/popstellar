import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { mockNavigate } from '__mocks__/useNavigationMock';
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
import STRINGS from 'resources/strings';

import { requestOpenRollCall, requestReopenRollCall } from '../../network';
import { RollCall, RollCallStatus } from '../../objects';
import EventRollCall from '../EventRollCall';

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

const mockRollCallClosed = RollCall.fromState(createStateWithStatus(RollCallStatus.CLOSED));
const mockRollCallCreated = RollCall.fromState(createStateWithStatus(RollCallStatus.CREATED));
const mockRollCallOpened = RollCall.fromState(createStateWithStatus(RollCallStatus.OPENED));

jest.mock('features/rollCall/network', () => {
  const actualNetwork = jest.requireActual('features/rollCall/network');
  return {
    ...actualNetwork,
    requestOpenRollCall: jest.fn(() => Promise.resolve()),
    requestReopenRollCall: jest.fn(() => Promise.resolve()),
  };
});

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

beforeEach(() => {
  jest.clearAllMocks();
});

describe('EventRollCall', () => {
  it('should correctly render', () => {
    mockStore.dispatch(updateRollCall(mockRollCallCreated.toState()));

    const Screen = () => (
      <EventRollCall eventId={mockRollCallCreated.id.valueOf()} isOrganizer={false} />
    );
    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Screen} />
        </FeatureContext.Provider>
      </Provider>,
    );

    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('should call requestOpenRollCall when the open button is clicked', () => {
    mockStore.dispatch(updateRollCall(mockRollCallCreated.toState()));

    const Screen = () => <EventRollCall eventId={mockRollCallCreated.id.valueOf()} isOrganizer />;
    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Screen} />
        </FeatureContext.Provider>
      </Provider>,
    );

    const openRollCallButton = obj.getByText(STRINGS.roll_call_open);
    fireEvent.press(openRollCallButton);
    expect(requestOpenRollCall).toHaveBeenCalledTimes(1);
  });

  it('should call requestReopenRollCall when the reopen button is clicked', () => {
    mockStore.dispatch(updateRollCall(mockRollCallClosed.toState()));

    const Screen = () => <EventRollCall eventId={mockRollCallClosed.id.valueOf()} isOrganizer />;
    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Screen} />
        </FeatureContext.Provider>
      </Provider>,
    );

    const reopenRollCallButton = obj.getByText(STRINGS.roll_call_reopen);
    fireEvent.press(reopenRollCallButton);
    expect(requestReopenRollCall).toHaveBeenCalledTimes(1);
  });

  it('should navigate to RollCallOpened when scan attendees button is clicked', () => {
    mockStore.dispatch(updateRollCall(mockRollCallOpened.toState()));

    const Screen = () => <EventRollCall eventId={mockRollCallOpened.id.valueOf()} isOrganizer />;
    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={Screen} />
        </FeatureContext.Provider>
      </Provider>,
    );

    const scanAttendeesButton = obj.getByText(STRINGS.roll_call_scan_attendees);
    fireEvent.press(scanAttendeesButton);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });
});
