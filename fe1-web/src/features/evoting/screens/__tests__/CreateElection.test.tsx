import { combineReducers, configureStore } from '@reduxjs/toolkit';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';

import MockNavigator from '__tests__/components/MockNavigator';
import {
  mockLao,
  mockLaoId,
  messageRegistryInstance,
  mockReduxAction,
  mockKeyPair,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EvotingReactContext, EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import { EventTags, Hash, Timestamp } from '../../../../core/objects';
import STRINGS from '../../../../resources/strings';
import { requestCreateElection } from '../../network/ElectionMessageApi';
import { ElectionVersion, Question, QuestionState } from '../../objects';
import { electionReducer, setDefaultQuestions } from '../../reducer';
import CreateElection from '../CreateElection';

jest.mock('features/evoting/network/ElectionMessageApi', () => {
  const actual = jest.requireActual('features/evoting/network/ElectionMessageApi');
  return {
    ...actual,
    requestCreateElection: jest.fn(() => Promise.resolve()),
  };
});

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    getCurrentLao: () => mockLao,
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    useCurrentLao: () => mockLao,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventFromId: () => undefined,
    messageRegistry: messageRegistryInstance,
    onConfirmEventCreation: () => undefined,
    getEventById: () => undefined,
    useLaoOrganizerBackendPublicKey: () => mockKeyPair.publicKey,
  } as EvotingReactContext,
};

const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...electionReducer,
  }),
});

mockStore.dispatch(setCurrentLao(mockLao));

describe('CreateElection', () => {
  it('renders correctly when question is empty', () => {
    const { getByTestId, toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={CreateElection} />
        </FeatureContext.Provider>
      </Provider>,
    );

    const nameInput = getByTestId('election_name_selector');
    fireEvent.changeText(nameInput, 'myElection');
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when name and question are empty', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={CreateElection} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly when name and questions are whitespace string', () => {
    const { getByTestId, toJSON } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={CreateElection} />
        </FeatureContext.Provider>
      </Provider>,
    );
    const electionName = getByTestId('election_name_selector');
    const question = getByTestId('question_selector_0_input');
    const option = getByTestId('question_0_ballots_option_0_input');
    fireEvent.changeText(electionName, '     ');
    fireEvent.changeText(question, '        ');
    fireEvent.changeText(option, '        ');
    waitFor(() => expect(toJSON()).toMatchSnapshot());
  });

  it('sends correctly data on edited input that should be trimmed', () => {
    const badQuestions: QuestionState[] = [
      {
        question: 'Question 0',
        ballot_options: ['Answer 0', 'Answer 1'],
        voting_method: STRINGS.election_method_Plurality,
        id: '',
        write_in: false,
      },
      {
        question: '   Question     1        ',
        ballot_options: ['    Answer 0    ', '  Answer 1  ', 'will be removed'],
        voting_method: STRINGS.election_method_Plurality,
        id: '',
        write_in: false,
      },
    ];

    mockStore.dispatch(setDefaultQuestions(badQuestions));
    const goodQuestions = [
      {
        question: 'Question 0',
        ballot_options: ['Answer 0', 'Answer 1'],
        voting_method: STRINGS.election_method_Plurality,
        id: '',
        write_in: false,
      },
    ];
    const electionId = Hash.fromArray(
      EventTags.ELECTION,
      mockLaoId,
      Timestamp.EpochNow(),
      'electionName',
    );

    const expectedQuestions = goodQuestions.map((item) => {
      return Question.fromState({
        id: Hash.fromArray(EventTags.QUESTION, electionId, item.question).toString(),
        question: item.question,
        ballot_options: item.ballot_options,
        voting_method: item.voting_method,
        // for now the write_in feature is disabled (2022-03-16, Tyratox)
        write_in: false,
      });
    });

    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={CreateElection} />
        </FeatureContext.Provider>
      </Provider>,
    );
    fireEvent.changeText(getByTestId('election_name_selector'), 'electionName');
    fireEvent.press(getByTestId('question_selector_1_remove'));
    fireEvent.press(getByTestId('election_confirm_selector'));
    expect(requestCreateElection).toHaveBeenCalledTimes(1);
    expect(requestCreateElection).toHaveBeenCalledWith(
      mockLaoId,
      'electionName',
      ElectionVersion.OPEN_BALLOT,
      Timestamp.EpochNow(),
      expect.anything(),
      expectedQuestions,
      Timestamp.EpochNow(),
    );
  });
});
