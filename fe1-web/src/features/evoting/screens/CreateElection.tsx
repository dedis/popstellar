import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Platform, Text, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import {
  ConfirmModal,
  DatePicker,
  DismissModal,
  DropdownSelector,
  Input,
  PoPTextButton,
  TextInputList,
} from 'core/components';
import { onChangeEndTime, onChangeStartTime } from 'core/components/DatePicker';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { onConfirmEventCreation } from 'core/functions/UI';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { EvotingHooks } from '../hooks';
import { EvotingFeature } from '../interface';
import { requestCreateElection } from '../network/ElectionMessageApi';
import { ElectionVersion, Question } from '../objects';

const DEFAULT_ELECTION_DURATION = 3600;

// for now only plurality voting is supported (2022-03-16, Tyratox)
const VOTING_METHOD = STRINGS.election_method_Plurality;

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_create_election>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

// the type used for storing questions in the react state
// does not yet contain the id of the questions, this is computed
// only on creation of the election
// ALSO: for now the write_in feature is disabled (2022-03-16, Tyratox)
type NewQuestion = Omit<Question, 'id' | 'write_in'>;

const EMPTY_QUESTION: NewQuestion = {
  question: '',
  voting_method: VOTING_METHOD,
  ballot_options: [''],
};

const MIN_BALLOT_OPTIONS = 2;

/**
 * Some helper functions
 */

/**
 * Checks whether a given newly created question is invalid
 * @param question The question to check
 */
const isQuestionInvalid = (question: NewQuestion): boolean =>
  question.question === '' || question.ballot_options.length < MIN_BALLOT_OPTIONS;

/**
 * Creates a new election based on the given values and returns the related request promise
 * @param laoId The id of the lao in which the new election should be created
 * @param version The version of the lection that should be created
 * @param electionName The name of the election
 * @param questions The questions created in the UI
 * @param startTime The start time of the election
 * @param endTime The end time of the election
 * @returns The promise returned by the requestCreateElection() function
 */
const createElection = (
  laoId: Hash,
  version: ElectionVersion,
  electionName: string,
  questions: NewQuestion[],
  startTime: Timestamp,
  endTime: Timestamp,
) => {
  // get the current time
  const now = Timestamp.EpochNow();

  // compute the id for the new election
  const electionId = Hash.fromStringArray(
    EventTags.ELECTION,
    laoId.toString(),
    now.toString(),
    electionName,
  );

  // compute the id for all questions and add the write_in property
  const questionsWithId = questions.map((item) => ({
    ...item,
    id: Hash.fromStringArray(EventTags.QUESTION, electionId.toString(), item.question).toString(),
    // for now the write_in feature is disabled (2022-03-16, Tyratox)
    write_in: false,
  }));

  return requestCreateElection(
    laoId,
    electionName,
    version,
    startTime,
    endTime,
    questionsWithId,
    now,
  );
};

/**
 * UI to create an Election Event
 */
const CreateElection = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();
  const currentLao = EvotingHooks.useCurrentLao();

  // form data for the new election
  const [startTime, setStartTime] = useState<Timestamp>(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState<Timestamp>(
    Timestamp.EpochNow().addSeconds(DEFAULT_ELECTION_DURATION),
  );
  const [electionName, setElectionName] = useState<string>('');

  const [questions, setQuestions] = useState<NewQuestion[]>([EMPTY_QUESTION]);
  const [version, setVersion] = useState<ElectionVersion>(ElectionVersion.OPEN_BALLOT);

  // UI state
  const [modalEndIsVisible, setModalEndIsVisible] = useState<boolean>(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState<boolean>(false);

  // Confirm button only clickable when the Name, Question and 2 Ballot options have values
  const buttonsVisibility: boolean = electionName !== '' && !questions.some(isQuestionInvalid);

  const onCreateElection = () => {
    createElection(currentLao.id, version, electionName, questions, startTime, endTime)
      .then(() => {
        navigation.navigate(STRINGS.navigation_lao_events_home);
      })
      .catch((err) => {
        console.error('Could not create Election, error:', err);
        toast.show(`Could not create Election, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const buildDatePickerWeb = () => {
    const startDate = startTime.toDate();
    const endDate = endTime.toDate();

    return (
      <>
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.election_create_start_time}
        </Text>
        <DatePicker
          selected={startDate}
          onChange={(date: Date) =>
            onChangeStartTime(date, setStartTime, setEndTime, DEFAULT_ELECTION_DURATION)
          }
        />

        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.election_create_finish_time}
        </Text>
        <DatePicker
          selected={endDate}
          onChange={(date: Date) => onChangeEndTime(date, startTime, setEndTime)}
        />
      </>
    );
  };

  const toolbarItems: ToolbarItem[] = [
    {
      id: 'election_confirm_selector',
      title: STRINGS.general_button_confirm,
      disabled: !buttonsVisibility,
      onPress: () =>
        onConfirmEventCreation(
          startTime,
          endTime,
          onCreateElection,
          setModalStartIsVisible,
          setModalEndIsVisible,
        ),
    },
  ];

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.election_create_name}
      </Text>
      <Input
        value={electionName}
        onChange={setElectionName}
        placeholder={STRINGS.election_create_name_placeholder}
        testID="election_name_selector"
      />
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.election_create_version}
      </Text>
      <DropdownSelector
        selected={version}
        onChange={(value) => {
          if (value) {
            setVersion(value as ElectionVersion);
          }
        }}
        options={[
          {
            value: ElectionVersion.OPEN_BALLOT,
            label: STRINGS.election_create_version_open_ballot,
          },
          {
            value: ElectionVersion.SECRET_BALLOT,
            label: STRINGS.election_create_version_secret_ballot,
          },
        ]}
      />
      {/* see archive branches for date picker used for native apps */}
      {Platform.OS === 'web' && buildDatePickerWeb()}
      {questions.map((value, idx) => (
        // FIXME: Do not use index in key
        // eslint-disable-next-line react/no-array-index-key
        <View key={idx.toString()}>
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.election_create_question} {idx + 1}
          </Text>
          <Input
            value={questions[idx].question}
            testID={`question_selector_${idx}`}
            onChange={(text: string) =>
              setQuestions((prev) =>
                prev.map((item, id) =>
                  id === idx
                    ? {
                        ...item,
                        question: text,
                      }
                    : item,
                ),
              )
            }
            placeholder={STRINGS.election_create_question_placeholder}
          />
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.election_create_ballot_options}
          </Text>
          <TextInputList
            placeholder={STRINGS.election_create_option_placeholder}
            onChange={(ballot_options: string[]) =>
              setQuestions((prev) =>
                prev.map((item, id) =>
                  id === idx
                    ? {
                        ...item,
                        ballot_options: ballot_options,
                      }
                    : item,
                ),
              )
            }
            testID={`question_${idx}_ballots`}
          />
        </View>
      ))}

      <PoPTextButton onPress={() => setQuestions((prev) => [...prev, EMPTY_QUESTION])}>
        {STRINGS.election_create_add_question}
      </PoPTextButton>

      <DismissModal
        visibility={modalEndIsVisible}
        setVisibility={setModalEndIsVisible}
        title={STRINGS.modal_event_creation_failed}
        description={STRINGS.modal_event_ends_in_past}
      />
      <ConfirmModal
        visibility={modalStartIsVisible}
        setVisibility={setModalStartIsVisible}
        title={STRINGS.modal_event_creation_failed}
        description={STRINGS.modal_event_starts_in_past}
        onConfirmPress={onCreateElection}
        buttonConfirmText={STRINGS.modal_button_start_now}
      />
    </ScreenWrapper>
  );
};

export default CreateElection;

export const CreateElectionScreen: EvotingFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_create_election,
  Component: CreateElection,
};
