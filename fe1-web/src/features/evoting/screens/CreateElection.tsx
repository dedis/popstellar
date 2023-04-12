import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo, useState } from 'react';
import { Platform, Text, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import {
  ConfirmModal,
  DatePicker,
  DismissModal,
  DropdownSelector,
  Input,
  PoPTextButton,
  RemovableTextInput,
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
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { EvotingHooks } from '../hooks';
import { EvotingFeature } from '../interface';
import { requestCreateElection } from '../network/ElectionMessageApi';
import { ElectionVersion, Question, QuestionState, EMPTY_QUESTION } from '../objects';
import { getElectionState } from '../reducer';

const DEFAULT_ELECTION_DURATION = 3600;

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.events_create_election>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

// the type used for storing questions in the react state
// does not yet contain the id of the questions, this is computed
// only on creation of the election
// ALSO: for now the write_in feature is disabled (2022-03-16, Tyratox)
type NewQuestion = Omit<QuestionState, 'id' | 'write_in'>;

const MIN_BALLOT_OPTIONS = 2;

/**
 * Some helper functions
 */

/**
 * Checks whether the question might be silently removed later
 * @param question The question to check
 * @param referenceQuestions The reference array
 */
const isSilentlyRemoved = (question: NewQuestion, referenceQuestions: NewQuestion[]): boolean =>
  question.question !== '' &&
  !referenceQuestions.some((trimmedQuestion) =>
    question.question.includes(trimmedQuestion.question),
  );

/**
 * Checks whether the question list has enough questions to be valid
 * @param questions The question list to check
 */
const hasEnoughQuestions = (questions: NewQuestion[]): boolean => questions.length > 0;

/**
 * Checks whether the ballot options of the question are invalid
 * @param question the question that contains the ballot options to check
 */
const hasInvalidBallotOptions = (question: NewQuestion): boolean => {
  // Impossible to do so without trimming. We do not have an id linking the non trimmed question to the trimmed one.
  const trimmedBallotOptions = question.ballot_options
    .map((ballot) => ballot.trim())
    .filter((ballot) => ballot !== '');
  return (
    question.ballot_options.length > 0 &&
    (new Set(trimmedBallotOptions).size !== question.ballot_options.length ||
      trimmedBallotOptions.length < MIN_BALLOT_OPTIONS)
  );
};

/**
 * Checks whether a question title is not unique within a list of questions
 * @param questions The list of questions
 */
const haveUniqueQuestionTitles = (questions: NewQuestion[]): boolean => {
  const questionTitles = questions.map((q: NewQuestion) => q.question);
  return questionTitles.length === new Set(questionTitles).size;
};

/**
 * Creates a new election based on the given values and returns the related request promise
 * @param laoId The id of the lao in which the new election should be created
 * @param version The version of the election that should be created
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
  const electionId = Hash.fromArray(EventTags.ELECTION, laoId, now, electionName);

  // compute the id for all questions and add the write_in property
  const questionsWithId = questions.map((item) =>
    Question.fromState({
      id: Hash.fromArray(EventTags.QUESTION, electionId, item.question).toString(),
      question: item.question,
      ballot_options: item.ballot_options,
      voting_method: item.voting_method,
      // for now the write_in feature is disabled (2022-03-16, Tyratox)
      write_in: false,
    }),
  );

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

const trimQuestion = (question: NewQuestion): NewQuestion => {
  return {
    ...question,
    question: question.question.trim(),
    ballot_options: question.ballot_options
      .map((val) => val.trim())
      .filter((value) => value !== ''),
  };
};

const globalErrorMessages = (
  isConnected: boolean | undefined,
  electionName: string,
  trimmedQuestions: NewQuestion[],
) => {
  return (
    <>
      {!isConnected && (
        <Text style={[Typography.paragraph, Typography.error]}>
          {STRINGS.event_creation_must_be_connected}
        </Text>
      )}
      {electionName.trim() === '' && (
        <Text style={[Typography.paragraph, Typography.error]}>
          {STRINGS.event_creation_name_not_empty}
        </Text>
      )}
      {!hasEnoughQuestions(trimmedQuestions) && (
        <Text style={[Typography.paragraph, Typography.error]}>
          {STRINGS.election_create_min_one_question}
        </Text>
      )}
      {!haveUniqueQuestionTitles(trimmedQuestions) && (
        <Text style={[Typography.paragraph, Typography.error]}>
          {STRINGS.election_create_same_questions}
        </Text>
      )}
    </>
  );
};

/**
 * UI to create an Election Event
 */
const CreateElection = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();
  const currentLao = EvotingHooks.useCurrentLao();
  const isConnected = EvotingHooks.useConnectedToLao();
  const defaultQuestions = useSelector((state) => getElectionState(state).defaultQuestions);

  // form data for the new election
  const [startTime, setStartTime] = useState<Timestamp>(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState<Timestamp>(
    Timestamp.EpochNow().addSeconds(DEFAULT_ELECTION_DURATION),
  );
  const [electionName, setElectionName] = useState<string>('');

  const [questions, setQuestions] = useState<NewQuestion[]>(defaultQuestions);
  const [version, setVersion] = useState<ElectionVersion>(ElectionVersion.OPEN_BALLOT);

  // UI state
  const [modalEndIsVisible, setModalEndIsVisible] = useState<boolean>(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState<boolean>(false);

  // Automatically compute the trimmed questions
  const trimmedQuestions = useMemo(() => {
    return questions.map(trimQuestion).filter((question) => question.question !== '');
  }, [questions]);

  // Confirm button only clickable when the Name, Question and 2 Ballot options have values
  const confirmButtonEnabled: boolean =
    isConnected === true &&
    electionName.trim() !== '' &&
    hasEnoughQuestions(trimmedQuestions) &&
    !questions.some((question) => isSilentlyRemoved(question, trimmedQuestions)) &&
    !questions.some(hasInvalidBallotOptions) &&
    haveUniqueQuestionTitles(trimmedQuestions);

  const onCreateElection = () => {
    createElection(
      currentLao.id,
      version,
      electionName.trim(),
      trimmedQuestions,
      startTime,
      endTime,
    )
      .then(() => {
        navigation.navigate(STRINGS.navigation_lao_events_home);
      })
      .catch((err) => {
        console.error('Could not create Election, error:', err);
        toast.show(`Could not create Election, error: ${err}`, {
          type: 'danger',
          placement: 'bottom',
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
      disabled: !confirmButtonEnabled,
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
      {questions.map((multipleChoiceQuestion, idx) => (
        // FIXME: Do not use index in key
        // eslint-disable-next-line react/no-array-index-key
        <View key={idx.toString()} style={{ marginBottom: Spacing.x2 }}>
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.election_create_question} {idx + 1}
          </Text>
          <RemovableTextInput
            value={multipleChoiceQuestion.question}
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
            onRemove={() => setQuestions((prev) => prev.filter((_, id) => id !== idx))}
            isRemovable={questions.length > 1}
            placeholder={STRINGS.election_create_question_placeholder}
          />
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.election_create_ballot_options}
          </Text>
          <TextInputList
            values={multipleChoiceQuestion.ballot_options}
            placeholder={STRINGS.election_create_option_placeholder}
            onChange={(ballot_options: string[]) =>
              setQuestions((prev) =>
                prev.map((item, id) =>
                  id === idx
                    ? {
                        ...item,
                        ballot_options: ballot_options.filter((option) => option !== ''),
                      }
                    : item,
                ),
              )
            }
            testID={`question_${idx}_ballots`}
          />
          {isSilentlyRemoved(multipleChoiceQuestion, trimmedQuestions) && (
            <Text style={[Typography.paragraph, Typography.error]}>
              {STRINGS.election_create_empty_question}
            </Text>
          )}
          {hasInvalidBallotOptions(multipleChoiceQuestion) && (
            <Text style={[Typography.paragraph, Typography.error]}>
              {STRINGS.election_create_invalid_ballot_options.replace(
                '{}',
                MIN_BALLOT_OPTIONS.toString(),
              )}
            </Text>
          )}
        </View>
      ))}
      <PoPTextButton onPress={() => setQuestions((prev) => [...prev, EMPTY_QUESTION])}>
        {STRINGS.election_create_add_question}
      </PoPTextButton>
      {globalErrorMessages(isConnected, electionName, trimmedQuestions)}
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
  id: STRINGS.events_create_election,
  Component: CreateElection,
};
