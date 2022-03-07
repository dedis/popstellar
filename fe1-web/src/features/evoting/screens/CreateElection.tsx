import React, { useState } from 'react';
import { View, Platform, ScrollView } from 'react-native';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';
import { useToast } from 'react-native-toast-notifications';

import STRINGS from 'resources/strings';
import { onChangeStartTime, onChangeEndTime } from 'core/components/DatePicker';
import {
  ConfirmModal,
  DatePicker,
  DismissModal,
  DropdownSelector,
  TextBlock,
  TextInputLine,
  TextInputList,
  ParagraphBlock,
  WideButtonView,
} from 'core/components';
import { Hash, Timestamp, EventTags } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';

import { requestCreateElection } from '../network/ElectionMessageApi';
import { Question } from '../objects';
import { EvotingHooks } from '../hooks';

const DEFAULT_ELECTION_DURATION = 3600;

/**
 * UI to create an Election Event
 */

const CreateElection = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();
  const toast = useToast();

  const [startTime, setStartTime] = useState(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState(
    Timestamp.EpochNow().addSeconds(DEFAULT_ELECTION_DURATION),
  );
  const [electionName, setElectionName] = useState('');
  const votingMethods = [STRINGS.election_method_Plurality, STRINGS.election_method_Approval];
  const minBallotOptions = 2;

  const emptyQuestion = { question: '', voting_method: votingMethods[0], ballot_options: [''] };
  const [questions, setQuestions] = useState([emptyQuestion]);
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);
  const time = Timestamp.EpochNow();

  const currentLao = EvotingHooks.useCurrentLao();
  const onConfirmEventCreation = EvotingHooks.useOnConfirmEventCreation();

  const buildDatePickerWeb = () => {
    const startDate = startTime.toDate();
    const endDate = endTime.toDate();

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.election_create_start_time} />
          <DatePicker
            selected={startDate}
            onChange={(date: Date) =>
              onChangeStartTime(date, setStartTime, setEndTime, DEFAULT_ELECTION_DURATION)
            }
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.election_create_finish_time} />
          <DatePicker
            selected={endDate}
            onChange={(date: Date) => onChangeEndTime(date, startTime, setEndTime)}
          />
        </View>
      </View>
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
        // @ts-ignore
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
      {/* see archive branches for date picker used for native apps */}
      {Platform.OS === 'web' && buildDatePickerWeb()}
      {questions.map((value, idx) => (
        <View key={idx.toString()}>
          <TextInputLine
            placeholder={STRINGS.election_create_question}
            onChangeText={(text: string) =>
              setQuestions((prev) =>
                prev.map((item, id) => (id === idx ? { ...item, question: text } : item)),
              )
            }
          />
          <View style={[styles.view, { marginHorizontal: 150 }]}>
            <ParagraphBlock text={STRINGS.election_voting_method} />
            <DropdownSelector
              values={votingMethods}
              onChange={(method: string) =>
                setQuestions((prev) =>
                  prev.map((item, id) => (id === idx ? { ...item, voting_method: method } : item)),
                )
              }
            />
          </View>
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
      ))}

      <View style={[styles.view, { zIndex: 'initial' }]}>
        <WideButtonView
          title="Add Question"
          onPress={() => setQuestions((prev) => [...prev, emptyQuestion])}
        />
        <WideButtonView title={STRINGS.general_button_cancel} onPress={navigation.goBack} />
        <WideButtonView
          title={STRINGS.general_button_confirm}
          onPress={() =>
            onConfirmEventCreation(
              startTime,
              endTime,
              createElection,
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
        onConfirmPress={() => createElection()}
        buttonConfirmText={STRINGS.modal_button_start_now}
        buttonCancelText={STRINGS.modal_button_go_back}
      />
    </ScrollView>
  );
};

export default CreateElection;
