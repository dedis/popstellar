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
  TextInputList,
  Button,
  Input,
} from 'core/components';
import { onChangeEndTime, onChangeStartTime } from 'core/components/DatePicker';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { onConfirmEventCreation } from 'core/functions/UI';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { EventTags, Hash, Timestamp } from 'core/objects';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { EvotingHooks } from '../hooks';
import { requestCreateElection } from '../network/ElectionMessageApi';
import { Question } from '../objects';

const DEFAULT_ELECTION_DURATION = 3600;

// for now only plurality voting is supported (2022-03-16, Tyratox)
const VOTING_METHOD = STRINGS.election_method_Plurality;

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_creation_election>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * UI to create an Election Event
 */
const CreateElection = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();

  const [startTime, setStartTime] = useState(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState(
    Timestamp.EpochNow().addSeconds(DEFAULT_ELECTION_DURATION),
  );
  const [electionName, setElectionName] = useState('');
  const minBallotOptions = 2;

  const emptyQuestion = { question: '', voting_method: VOTING_METHOD, ballot_options: [''] };
  const [questions, setQuestions] = useState([emptyQuestion]);
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);
  const time = Timestamp.EpochNow();

  const currentLao = EvotingHooks.useCurrentLao();

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

  const electionId = Hash.fromStringArray(
    EventTags.ELECTION,
    currentLao.id.toString(),
    time.toString(),
    electionName,
  );
  const getQuestionObjects = (): Question[] =>
    questions.map((item) => ({
      ...item,
      id: Hash.fromStringArray(EventTags.QUESTION, electionId.toString(), item.question).toString(),
      // for now the write_in feature is disabled (2022-03-16, Tyratox)
      write_in: false,
    }));

  const isInvalid = (obj: Question): boolean =>
    obj.question === '' || obj.ballot_options.length < minBallotOptions;

  // Confirm button only clickable when the Name, Question and 2 Ballot options have values
  const buttonsVisibility: boolean = electionName !== '' && !getQuestionObjects().some(isInvalid);

  const createElection = () => {
    requestCreateElection(
      currentLao.id,
      electionName,
      STRINGS.election_version_identifier,
      startTime,
      endTime,
      getQuestionObjects(),
      time,
    )
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

  return (
    <ScreenWrapper>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.election_create_setup}
      </Text>
      <Input
        value={electionName}
        onChange={setElectionName}
        placeholder={STRINGS.election_create_setup}
      />

      {/* see archive branches for date picker used for native apps */}
      {Platform.OS === 'web' && buildDatePickerWeb()}
      {questions.map((value, idx) => (
        <View key={idx.toString()}>
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.election_create_question} {idx + 1}
          </Text>
          <Input
            value={questions[idx].question}
            onChange={(text: string) =>
              setQuestions((prev) =>
                prev.map((item, id) => (id === idx ? { ...item, question: text } : item)),
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
                  id === idx ? { ...item, ballot_options: ballot_options } : item,
                ),
              )
            }
          />
        </View>
      ))}

      <Button onPress={() => setQuestions((prev) => [...prev, emptyQuestion])}>
        <Text style={[Typography.base, Typography.centered, Typography.negative]}>
          {STRINGS.election_create_add_question}
        </Text>
      </Button>

      <Button
        onPress={() =>
          onConfirmEventCreation(
            startTime,
            endTime,
            createElection,
            setModalStartIsVisible,
            setModalEndIsVisible,
          )
        }
        disabled={!buttonsVisibility}>
        <Text style={[Typography.base, Typography.centered, Typography.negative]}>
          {STRINGS.general_button_confirm}
        </Text>
      </Button>

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
        onConfirmPress={() => createElection()}
        buttonConfirmText={STRINGS.modal_button_start_now}
        buttonCancelText={STRINGS.modal_button_go_back}
      />
    </ScreenWrapper>
  );
};

export default CreateElection;
