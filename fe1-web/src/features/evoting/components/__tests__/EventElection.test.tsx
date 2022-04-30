import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import {
  mockLao,
  mockLaoId,
  mockLaoIdHash,
  mockLaoName,
  messageRegistryInstance,
  mockReduxAction,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';
import {
  Election,
  ElectionStatus,
  ElectionVersion,
  Question,
  QuestionResult,
  RegisteredVote,
} from 'features/evoting/objects';
import { electionKeyReducer } from 'features/evoting/reducer';
import STRINGS from 'resources/strings';

import EventElection from '../EventElection';

// region test data initialization

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

const registeredVote: RegisteredVote = {
  createdAt: 0,
  messageId: '',
  sender: '',
  votes: [{ id: '', question: '', vote: [0] }],
};

const questionResult: QuestionResult = {
  id: '',
  result: [
    { ballotOption: mockBallotOptions[0], count: 10 },
    { ballotOption: mockBallotOptions[1], count: 3 },
  ],
};

const notStartedElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.NOT_STARTED,
  registeredVotes: [],
});

const notStartedSecretBallotElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.SECRET_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.NOT_STARTED,
  registeredVotes: [],
});

const runningElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.OPENED,
  registeredVotes: [registeredVote],
});

const terminatedElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.TERMINATED,
  registeredVotes: [registeredVote],
});

const resultElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: ElectionStatus.RESULT,
  registeredVotes: [registeredVote],
  questionResult: [questionResult],
});

const undefinedElection = new Election({
  lao: mockLaoIdHash,
  id: mockElectionId,
  name: 'An election',
  version: ElectionVersion.OPEN_BALLOT,
  createdAt: TIMESTAMP,
  start: TIMESTAMP,
  end: CLOSE_TIMESTAMP,
  questions: [question],
  electionStatus: 'undefined' as ElectionStatus,
  registeredVotes: [registeredVote],
  questionResult: [questionResult],
});

// endregion

// mocks

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

const mockStore = createStore(combineReducers(electionKeyReducer));

describe('EventElection', () => {
  describe('Not started election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={notStartedElection} isOrganizer />,
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={notStartedElection} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Running election', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={runningElection} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={runningElection} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Terminated election where the results are not yet available', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={terminatedElection} isOrganizer />,
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={terminatedElection} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Finished election where the results are available', () => {
    it('renders correctly for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={resultElection} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={resultElection} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });

  describe('Undefined election status', () => {
    it('renders null for an organizer', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={undefinedElection} isOrganizer />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders null for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={undefinedElection} />
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
            <EventElection event={notStartedSecretBallotElection} isOrganizer />,
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });

    it('renders correctly for an attendee', () => {
      const component = render(
        <Provider store={mockStore}>
          <FeatureContext.Provider value={contextValue}>
            <EventElection event={notStartedSecretBallotElection} />
          </FeatureContext.Provider>
        </Provider>,
      ).toJSON();
      expect(component).toMatchSnapshot();
    });
  });
});
