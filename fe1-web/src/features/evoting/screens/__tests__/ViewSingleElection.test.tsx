import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import {
  mockLao,
  mockLaoIdHash,
  messageRegistryInstance,
  mockReduxAction,
  mockLaoId,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Timestamp } from 'core/objects';
import { addEvent } from 'features/events/reducer';
import {
  mockElectionNotStarted,
  mockElectionOpened,
  mockElectionResults,
  mockElectionTerminated,
  openedSecretBallotElection,
} from 'features/evoting/__tests__/utils';
import { EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';
import { Election, ElectionStatus } from 'features/evoting/objects';
import {
  addElection,
  electionKeyReducer,
  electionReducer,
  updateElection,
} from 'features/evoting/reducer';

import ViewSingleElection, { ViewSingleElectionScreenHeader } from '../ViewSingleElection';

jest.spyOn(Date.prototype, 'toLocaleDateString').mockReturnValue('2022-05-28');
jest.spyOn(Date.prototype, 'toLocaleTimeString').mockReturnValue('00:00:00');

jest.useFakeTimers('modern');
jest.setSystemTime(new Date(Timestamp.EpochNow().valueOf() * 1000)); // 5 May 2021

// Icons and snapshot tests do not work nice together
// See https://github.com/expo/expo/issues/3566
jest.mock('@expo/vector-icons');

const undefinedElection = Election.fromState({
  ...mockElectionNotStarted.toState(),
  electionStatus: 'undefined' as ElectionStatus,
});

// mocks
const mockStore = createStore(combineReducers({ ...electionReducer, ...electionKeyReducer }));
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
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventById: () => undefined,
    messageRegistry: messageRegistryInstance,
    onConfirmEventCreation: () => undefined,
  },
};

describe('ViewSingleElection', () => {
  const testRender = (election: Election, isOrganizer: boolean) => () => {
    mockStore.dispatch(updateElection(election.toState()));

    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSingleElection}
            params={{ eventId: election.id.valueOf(), isOrganizer }}
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

describe('ViewSinglRollCallScreenRightHeader', () => {
  const testRender = (election: Election, isOrganizer: boolean) => () => {
    mockStore.dispatch(updateElection(election.toState()));

    const obj = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSingleElectionScreenHeader}
            params={{ eventId: election.id.valueOf(), isOrganizer }}
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
