import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import * as reactRedux from 'react-redux';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { mockNavigate } from '__mocks__/useNavigationMock';
import MockNavigator from '__tests__/components/MockNavigator';
import { configureTestFeatures } from '__tests__/utils';
import { mockLao } from '__tests__/utils/TestUtils';
import { Hash, Timestamp } from 'core/objects';
import { connectToLao, laoReducer } from 'features/lao/reducer';
import STRINGS from 'resources/strings';

import { requestOpenRollCall, requestReopenRollCall } from '../../network';
import { EventTypeRollCall, RollCall, RollCallStatus } from '../../objects';
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
    eventType: EventTypeRollCall,
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

let mockSelector: any;

beforeAll(() => {
  // the wallet uses the global store hence the full test features are required
  configureTestFeatures();
});

// set up mock store
const mockStore = createStore(combineReducers(laoReducer));
mockStore.dispatch(connectToLao(mockLao.toState()));

beforeEach(() => {
  jest.clearAllMocks();
  mockSelector = jest.spyOn(reactRedux, 'useSelector').mockReturnValueOnce(mockLao);
});

describe('EventRollCall', () => {
  it('should correctly render', () => {
    mockSelector.mockReturnValueOnce(mockRollCallCreated);

    const Screen = () => <EventRollCall event={mockRollCallCreated} isOrganizer={false} />;
    const obj = render(
      <Provider store={mockStore}>
        <MockNavigator component={Screen} />
      </Provider>,
    );

    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('should call requestOpenRollCall when the open button is clicked', () => {
    const usedMockRollCall = mockRollCallCreated;
    mockSelector.mockReturnValueOnce(usedMockRollCall);

    const Screen = () => <EventRollCall event={usedMockRollCall} isOrganizer />;
    const obj = render(
      <Provider store={mockStore}>
        <Provider store={mockStore}>
          <MockNavigator component={Screen} />
        </Provider>
      </Provider>,
    );

    const openRollCallButton = obj.getByText(STRINGS.roll_call_open);
    fireEvent.press(openRollCallButton);
    expect(requestOpenRollCall).toHaveBeenCalledTimes(1);
  });

  it('should call requestReopenRollCall when the reopen button is clicked', () => {
    const usedMockRollCall = mockRollCallClosed;
    mockSelector.mockReturnValueOnce(usedMockRollCall);

    const Screen = () => <EventRollCall event={usedMockRollCall} isOrganizer />;
    const obj = render(
      <Provider store={mockStore}>
        <MockNavigator component={Screen} />
      </Provider>,
    );

    const reopenRollCallButton = obj.getByText(STRINGS.roll_call_reopen);
    fireEvent.press(reopenRollCallButton);
    expect(requestReopenRollCall).toHaveBeenCalledTimes(1);
  });

  it('should navigate to RollCallOpened when scan attendees button is clicked', () => {
    const usedMockRollCall = mockRollCallOpened;
    mockSelector.mockReturnValueOnce(usedMockRollCall);

    const Screen = () => <EventRollCall event={usedMockRollCall} isOrganizer />;
    const obj = render(
      <Provider store={mockStore}>
        <MockNavigator component={Screen} />
      </Provider>,
    );

    const scanAttendeesButton = obj.getByText(STRINGS.roll_call_scan_attendees);
    fireEvent.press(scanAttendeesButton);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });
});
