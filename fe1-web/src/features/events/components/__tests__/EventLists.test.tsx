import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockKeyPair, mockLao, serializedMockLaoId, mockLaoId, mockLaoName } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { getEventById } from 'features/events/functions';
import { EVENT_FEATURE_IDENTIFIER, EventReactContext } from 'features/events/interface';
import {
  addEvent,
  eventReducer,
  makeEventByTypeSelector,
  updateEvent,
} from 'features/events/reducer';
import { mockElectionNotStarted } from 'features/evoting/__tests__/utils';
import { ElectionEventType } from 'features/evoting/components';
import { EVOTING_FEATURE_IDENTIFIER, EvotingReactContext } from 'features/evoting/interface';
import { Election } from 'features/evoting/objects';
import { addElection, electionReducer } from 'features/evoting/reducer';
import { mockMeeting } from 'features/meeting/__tests__/utils';
import { MeetingEventType } from 'features/meeting/components';
import { MEETING_FEATURE_IDENTIFIER, MeetingReactContext } from 'features/meeting/interface';
import { Meeting } from 'features/meeting/objects';
import { addMeeting, meetingReducer } from 'features/meeting/reducer';
import { mockRollCall } from 'features/rollCall/__tests__/utils';
import { RollCallEventType } from 'features/rollCall/components';
import { ROLLCALL_FEATURE_IDENTIFIER, RollCallReactContext } from 'features/rollCall/interface';
import { RollCall } from 'features/rollCall/objects';
import { addRollCall, rollCallReducer } from 'features/rollCall/reducer';
import { WALLET_FEATURE_IDENTIFIER, WalletReactContext } from 'features/wallet/interface';
import { generateToken } from 'features/wallet/objects';
import { getWalletState, walletReducer } from 'features/wallet/reducer';

import EventLists from '../EventLists';

const mockStore = configureStore({
  reducer: combineReducers({
    ...eventReducer,
    ...electionReducer,
    ...meetingReducer,
    ...rollCallReducer,
    ...walletReducer,
  }),
});

const emptyStore = configureStore({
  reducer: combineReducers({
    ...eventReducer,
    ...electionReducer,
    ...meetingReducer,
    ...rollCallReducer,
    ...walletReducer,
  }),
});

const getContextValue = (isOrganizer: boolean) => ({
  [EVENT_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    eventTypes: [ElectionEventType, MeetingEventType, RollCallEventType],
    useIsLaoOrganizer: () => isOrganizer,
  } as EventReactContext,
  [EVOTING_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    useCurrentLao: () => mockLao,
    addEvent,
    updateEvent,
    getEventById,
    useLaoOrganizerBackendPublicKey: () => mockKeyPair.publicKey,
  } as EvotingReactContext,
  [MEETING_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
  } as MeetingReactContext,
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    generateToken,
    hasSeed: () => getWalletState(mockStore.getState()).seed !== undefined,
    makeEventByTypeSelector,
  } as RollCallReactContext,
  [WALLET_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useCurrentLao: () => mockLao,
    useConnectedToLao: () => true,
    useRollCallsByLaoId: () => ({}),
    useLaoIds: () => [mockLaoId],
    useNamesByLaoId: () => ({ [serializedMockLaoId]: mockLaoName }),
    useRollCallTokensByLaoId: () => [],
  } as WalletReactContext,
});

mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: Election.EVENT_TYPE,
    id: mockElectionNotStarted.id.valueOf(),
    start: mockElectionNotStarted.start.valueOf(),
    end: mockElectionNotStarted.end.valueOf(),
  }),
);
mockStore.dispatch(addElection(mockElectionNotStarted.toState()));

mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: Meeting.EVENT_TYPE,
    id: mockMeeting.id.valueOf(),
    start: mockMeeting.start.valueOf(),
    end: mockMeeting.end?.valueOf(),
  }),
);
mockStore.dispatch(addMeeting(mockMeeting.toState()));

mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: RollCall.EVENT_TYPE,
    id: mockRollCall.id.valueOf(),
    start: mockRollCall.start.valueOf(),
    end: mockRollCall.end?.valueOf(),
  }),
);
mockStore.dispatch(addRollCall(mockRollCall.toState()));

describe('EventLists', () => {
  it('renders correctly for attendees', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={getContextValue(false)}>
          <MockNavigator component={EventLists} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly for organizers', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={getContextValue(true)}>
          <MockNavigator component={EventLists} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('displays welcome message when no events are present for organizer', () => {
    const component = render(
      <Provider store={emptyStore}>
        <FeatureContext.Provider value={getContextValue(true)}>
          <MockNavigator component={EventLists} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('displays welcome message when no events are present for attendee', () => {
    const component = render(
      <Provider store={emptyStore}>
        <FeatureContext.Provider value={getContextValue(false)}>
          <MockNavigator component={EventLists} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
