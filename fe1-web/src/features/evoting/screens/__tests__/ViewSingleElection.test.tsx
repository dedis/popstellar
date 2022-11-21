import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import {
  messageRegistryInstance,
  mockKeyPair,
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockReduxAction,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { addEvent } from 'features/events/reducer';
import {
  mockElectionNotStarted,
  mockElectionOpened,
  mockElectionResults,
  mockElectionTerminated,
  openedSecretBallotElection,
} from 'features/evoting/__tests__/utils';
import { EvotingReactContext, EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';
import { Election, ElectionStatus } from 'features/evoting/objects';
import {
  addElection,
  electionKeyReducer,
  electionReducer,
  updateElection,
} from 'features/evoting/reducer';

import ViewSingleElection from '../ViewSingleElection';

const undefinedElection = Election.fromState({
  ...mockElectionNotStarted.toState(),
  electionStatus: 'undefined' as ElectionStatus,
});

// mocks
const mockStore = configureStore({
  reducer: combineReducers({
    ...electionReducer,
    ...electionKeyReducer,
  }),
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

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    useCurrentLaoId: () => mockLaoIdHash,
    useConnectedToLao: () => true,
    useLaoOrganizerBackendPublicKey: () => mockKeyPair.publicKey,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventById: () => undefined,
    messageRegistry: messageRegistryInstance,
    onConfirmEventCreation: () => undefined,
  } as EvotingReactContext,
};

describe('ViewSingleElection', () => {
  const testRender = (election: Election, isOrganizer: boolean) => () => {
    mockStore.dispatch(updateElection(election.toState()));

    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSingleElection}
            params={{
              eventId: election.id.valueOf(),
              isOrganizer,
            }}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    expect(obj.toJSON()).toMatchSnapshot();
  };

  describe('renders correctly', () => {
    describe('organizers', () => {
      it('not started election', testRender(mockElectionNotStarted, true));
      it('opened election', testRender(mockElectionOpened, true));
      it('terminated election', testRender(mockElectionTerminated, true));
      it('election with results', testRender(mockElectionResults, true));
      it('undefined election status', testRender(undefinedElection, true));

      it('open secret ballot election', testRender(openedSecretBallotElection, true));
    });

    describe('non organizers', () => {
      it('not started election', testRender(mockElectionNotStarted, false));
      it('opened election', testRender(mockElectionOpened, false));
      it('terminated election', testRender(mockElectionTerminated, false));
      it('election with results', testRender(mockElectionResults, false));
      it('undefined election status', testRender(undefinedElection, false));

      it('open secret ballot election', testRender(openedSecretBallotElection, false));
    });
  });
});
