import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

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

import CreateElection from '../CreateElection';

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

describe('CreateElection', () => {
  it('renders correctly when question is empty', () => {
    const { getByTestId, toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateElection} />
      </FeatureContext.Provider>,
    );

    const nameInput = getByTestId('election_name_selector');
    fireEvent.changeText(nameInput, 'myElection');
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when name and question are empty', () => {
    const component = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateElection} />
      </FeatureContext.Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly when name and questions are whitespace string', () => {
    const { getByTestId, toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateElection} />
      </FeatureContext.Provider>,
    );
    const question = getByTestId('election_name_selector');
    fireEvent.changeText(question, '     ');
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly trimmed question and election name', () => {
    const { getByTestId, toJSON } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateElection} />
      </FeatureContext.Provider>,
    );
    const electionName = getByTestId('election_name_selector');
    const question = getByTestId('question_selector_0');
    fireEvent.changeText(question, '    Trimmed question ');
    fireEvent.changeText(electionName, '    Trimmed election name    ');
    expect(toJSON()).toMatchSnapshot();
  });
  /** TODO: Fix this test
  it('sends correctly data on input that should be trimmed', async () => {
    const { getByTestId } = render(
      <FeatureContext.Provider value={contextValue}>
        <MockNavigator component={CreateElection} />
      </FeatureContext.Provider>,
    );

    const questions: Omit<QuestionState, 'id' | 'write_in'>[] = [
      {
        question: 'Question 0',
        ballot_options: ['Answer 0', 'Answer 1'],
        voting_method: STRINGS.election_method_Plurality,
      },
    ];
    jest.setSystemTime(Date.UTC(2023, 3, 21));

    fireEvent.changeText(getByTestId('election_name_selector'), '   Election name   ');
    fireEvent.changeText(getByTestId('question_selector_0'), '   Question 0   ');
    fireEvent.changeText(getByTestId('question_0_ballots_option_0'), '   Answer 0   ');
    fireEvent.changeText(getByTestId('question_0_ballots_option_1'), '   Answer 1   ');
    fireEvent.changeText(getByTestId('question_0_ballots_option_2'), '      ');
    fireEvent.press(getByTestId('election_confirm_selector'));
    await waitFor(() => {
      expect(createElection).toHaveBeenCalledWith(
        mockLaoId,
        ElectionVersion.OPEN_BALLOT,
        'Election name',
        questions,
        Timestamp.EpochNow(),
        // might change
        Timestamp.EpochNow().addSeconds(3600),
      );
      expect(createElection).toHaveBeenCalledTimes(1);
    });
  }); */
});
