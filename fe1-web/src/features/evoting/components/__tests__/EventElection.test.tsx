import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import {
  mockLao,
  mockLaoIdHash,
  messageRegistryInstance,
  mockReduxAction,
  mockLaoId,
  mockLaoName,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { addEvent } from 'features/events/reducer';
import {
  mockElectionNotStarted,
  mockElectionOpened,
  mockElectionResults,
  mockElectionTerminated,
} from 'features/evoting/__tests__/utils';
import { EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';
import { Election, ElectionStatus, ElectionVersion, Question } from 'features/evoting/objects';
import {
  addElection,
  electionKeyReducer,
  electionReducer,
  updateElection,
} from 'features/evoting/reducer';
import STRINGS from 'resources/strings';

import EventElection from '../EventElection';

const undefinedElection = Election.fromState({
  ...mockElectionNotStarted.toState(),
  electionStatus: 'undefined' as ElectionStatus,
});

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021

const mockElectionId = Hash.fromStringArray(
  'Election',
  mockLaoId,
  TIMESTAMP.toString(),
  mockLaoName,
);

const mockQuestion = 'Mock Question 1';
const mockQuestionId = Hash.fromStringArray(
  EventTags.QUESTION,
  mockElectionId.toString(),
  mockQuestion,
);
const mockBallotOptions = ['Ballot Option 1', 'Ballot Option 2'];

const question: Question = {
  id: mockQuestionId.toString(),
  question: mockQuestion,
  voting_method: STRINGS.election_method_Plurality,
  ballot_options: mockBallotOptions,
  write_in: false,
};

const openedSecretBallotElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.SECRET_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.OPENED,
  registeredVotes: [],
});

// endregion

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

describe('EventElection', () => {
  describe('Not started election', () => {
    beforeAll(() => {
      mockStore.dispatch(updateElection(mockElectionNotStarted.toState()));
    });

    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionNotStarted.id.valueOf()} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionNotStarted.id.valueOf()} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Opened election', () => {
    beforeAll(() => {
      mockStore.dispatch(updateElection(mockElectionOpened.toState()));
    });

    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionOpened.id.valueOf()} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionOpened.id.valueOf()} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Terminated election where the results are not yet available', () => {
    beforeAll(() => {
      mockStore.dispatch(updateElection(mockElectionTerminated.toState()));
    });

    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionTerminated.id.valueOf()} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionTerminated.id.valueOf()} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Finished election where the results are available', () => {
    beforeAll(() => {
      mockStore.dispatch(updateElection(mockElectionResults.toState()));
    });

    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionResults.id.valueOf()} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={mockElectionResults.id.valueOf()} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Undefined election status', () => {
    beforeAll(() => {
      mockStore.dispatch(updateElection(undefinedElection.toState()));
    });

    it('renders null for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={undefinedElection.id.valueOf()} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders null for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={undefinedElection.id.valueOf()} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Secret ballot election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={openedSecretBallotElection.id.valueOf()} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection eventId={openedSecretBallotElection.id.valueOf()} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });
});
