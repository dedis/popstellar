import 'react-datepicker/dist/react-datepicker.css';

import { useNavigation } from '@react-navigation/native';
import React, { useState } from 'react';
import { Platform, ScrollView, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import {
  ConfirmModal,
  DatePicker,
  DismissModal,
  ParagraphBlock,
  TextBlock,
  TextInputLine,
  TextInputList,
  WideButtonView,
} from 'core/components';
import { onChangeEndTime, onChangeStartTime } from 'core/components/DatePicker';
import { onConfirmEventCreation } from 'core/functions/UI';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { EvotingHooks } from '../hooks';
import { requestCreateElection } from '../network/ElectionMessageApi';
import { ElectionVersion, Question } from '../objects';

const DEFAULT_ELECTION_DURATION = 3600;

// for now only plurality voting is supported (2022-03-16, Tyratox)
const VOTING_METHOD = STRINGS.election_method_Plurality;

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

const CreateElection = ({ route }: any) => {
  const styles = route.params;

  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();
  const toast = useToast();
  const currentLao = EvotingHooks.useCurrentLao();

  // form data for the new election
  const [startTime, setStartTime] = useState<Timestamp>(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState<Timestamp>(
    Timestamp.EpochNow().addSeconds(DEFAULT_ELECTION_DURATION),
  );
  const [electionName, setElectionName] = useState<string>('');

  const [questions, setQuestions] = useState<NewQuestion[]>([EMPTY_QUESTION]);
  const [version] = useState<ElectionVersion>(ElectionVersion.OPEN_BALLOT);

  // UI state
  const [modalEndIsVisible, setModalEndIsVisible] = useState<boolean>(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState<boolean>(false);

  // Confirm button only clickable when the Name, Question and 2 Ballot options have values
  const buttonsVisibility: boolean = electionName !== '' && !questions.some(isQuestionInvalid);

  const onCreateElection = () => {
    createElection(currentLao.id, version, electionName, questions, startTime, endTime)
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
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

  return (
    <ScrollView>
      <TextBlock text={STRINGS.election_create_setup} bold />
      <TextInputLine
        placeholder={STRINGS.election_create_name}
        onChangeText={(text: string) => {
          setElectionName(text);
        }}
      />
      {
        // the date picker for the web
        // see archive branches for date picker used for native apps
        Platform.OS === 'web' && (
          <View style={styles.viewVertical}>
            <View style={[styles.view, { padding: 5 }]}>
              <ParagraphBlock text={STRINGS.election_create_start_time} />
              <DatePicker
                selected={startTime.toDate()}
                onChange={(date: Date) =>
                  onChangeStartTime(date, setStartTime, setEndTime, DEFAULT_ELECTION_DURATION)
                }
              />
            </View>
            <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
              <ParagraphBlock text={STRINGS.election_create_finish_time} />
              <DatePicker
                selected={endTime.toDate()}
                onChange={(date: Date) => onChangeEndTime(date, startTime, setEndTime)}
              />
            </View>
          </View>
        )
      }
      {
        // list all questions
        questions.map((_, idx) => (
          <View key={idx.toString()}>
            <TextInputLine
              placeholder={STRINGS.election_create_question}
              onChangeText={(text: string) =>
                setQuestions((prev) =>
                  prev.map((item, id) => (id === idx ? { ...item, question: text } : item)),
                )
              }
            />
            <TextInputList
              onChange={(ballot_options: string[]) =>
                setQuestions((prev) =>
                  prev.map((item, id) =>
                    id === idx ? { ...item, ballot_options: ballot_options } : item,
                  ),
                )
              }
            />
          </View>
        ))
      }

      <View style={[styles.view, { zIndex: 'initial' }]}>
        <WideButtonView
          title="Add Question"
          onPress={() => setQuestions((prev) => [...prev, EMPTY_QUESTION])}
        />
        <WideButtonView title={STRINGS.general_button_cancel} onPress={navigation.goBack} />
        <WideButtonView
          title={STRINGS.general_button_confirm}
          onPress={() =>
            onConfirmEventCreation(
              startTime,
              endTime,
              onCreateElection,
              setModalStartIsVisible,
              setModalEndIsVisible,
            )
          }
          disabled={!buttonsVisibility}
        />
      </View>

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
        onConfirmPress={() => onCreateElection()}
        buttonConfirmText={STRINGS.modal_button_start_now}
        buttonCancelText={STRINGS.modal_button_go_back}
      />
    </ScrollView>
  );
};

export default CreateElection;
