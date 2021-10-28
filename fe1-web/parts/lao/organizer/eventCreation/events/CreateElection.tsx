import React, { useState } from 'react';
import {
  View, Platform, ScrollView, Modal,
} from 'react-native';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';

import STRINGS from 'res/strings';
import DatePicker, { onChangeStartTime, onChangeEndTime } from 'components/DatePicker';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import TextBlock from 'components/TextBlock';
import DropdownSelector from 'components/DropdownSelector';
import TextInputList from 'components/TextInputList';
import TextInputLine from 'components/TextInputLine';
import {
  Hash, Lao, Timestamp, Question, EventTags,
} from 'model/objects';
import { requestCreateElection } from 'network';
import { OpenedLaoStore } from 'store';
import { onConfirmPress } from '../CreateEvent';

const DEFAULT_ELECTION_DURATION = 3600;

/**
 * UI to create an Election Event
 */

const CreateElection = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();

  const [startTime, setStartTime] = useState(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState(Timestamp.EpochNow()
    .addSeconds(DEFAULT_ELECTION_DURATION));
  const [electionName, setElectionName] = useState('');
  const votingMethods = [STRINGS.election_method_Plurality, STRINGS.election_method_Approval];
  const minBallotOptions = 2;
  const currentLao: Lao = OpenedLaoStore.get();
  const emptyQuestion = { question: '', voting_method: votingMethods[0], ballot_options: [''] };
  const [questions, setQuestions] = useState([emptyQuestion]);
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);

  const buildDatePickerWeb = () => {
    const startDate = startTime.timestampToDate();
    const endDate = endTime.timestampToDate();

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.election_create_start_time} />
          <DatePicker
            selected={startDate}
            onChange={(date: Date) => onChangeStartTime(date, setStartTime, setEndTime,
              DEFAULT_ELECTION_DURATION)}
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

  const getQuestionObjects = (): Question[] => questions.map((item) => (
    {
      ...item,
      id: Hash.fromStringArray(
        EventTags.QUESTION, currentLao.id.toString(), item.question,
      ).toString(),
      write_in: false,
    }));

  const isInvalid = (obj: Question): boolean => (obj.question === ''
    || obj.ballot_options.length < minBallotOptions);

  // Confirm button only clickable when the Name, Question and 2 Ballot options have values
  const buttonsVisibility: boolean = (electionName !== ''
    && !getQuestionObjects().some(isInvalid));

  const createElection = () => {
    console.log(getQuestionObjects());
    requestCreateElection(
      electionName,
      STRINGS.election_version_identifier,
      startTime,
      endTime,
      getQuestionObjects(),
    )
      .then(() => {
        // @ts-ignore
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not create Election, error:', err);
      });
  };

  return (
    <ScrollView>
      <TextBlock text={STRINGS.election_create_setup} bold />
      <TextInputLine
        placeholder={STRINGS.election_create_name}
        onChangeText={(text: string) => { setElectionName(text); }}
      />
      { /* see archive branches for date picker used for native apps */ }
      { Platform.OS === 'web' && buildDatePickerWeb() }
      { questions.map((value, idx) => (
        <View key={idx.toString()}>
          <TextInputLine
            placeholder={STRINGS.election_create_question}
            onChangeText={(text: string) => setQuestions(
              (prev) => prev.map((item, id) => (
                (id === idx) ? { ...item, question: text } : item)),
            )}
          />
          <View style={[styles.view, { marginHorizontal: 150 }]}>
            <ParagraphBlock text={STRINGS.election_voting_method} />
            <DropdownSelector
              values={votingMethods}
              onChange={(method: string) => setQuestions(
                (prev) => prev.map((item, id) => (
                  (id === idx) ? { ...item, voting_method: method } : item)),
              )}
            />
          </View>
          <TextInputList onChange={(ballot_options: string[]) => setQuestions(
            (prev) => prev.map((item, id) => (
              (id === idx) ? { ...item, ballot_options: ballot_options } : item)),
          )}
          />
        </View>
      ))}

      <View style={[styles.view, { zIndex: 'initial' }]}>
        <WideButtonView
          title="Add Question"
          onPress={() => setQuestions((prev) => [...prev, emptyQuestion])}
        />
        <WideButtonView
          title={STRINGS.general_button_cancel}
          onPress={navigation.goBack}
        />
        <WideButtonView
          title={STRINGS.general_button_confirm}
          onPress={() => onConfirmPress(startTime, endTime, createElection, setModalStartIsVisible,
            setModalEndIsVisible)}
          disabled={!buttonsVisibility}
        />
      </View>

      <Modal
        visible={modalEndIsVisible}
        onRequestClose={() => setModalEndIsVisible(!modalEndIsVisible)}
        transparent
      >
        <View style={styles.modalView}>
          <TextBlock text={STRINGS.modal_event_creation_failed} bold />
          <TextBlock text={STRINGS.modal_event_ends_in_past} />
          <WideButtonView
            title={STRINGS.general_button_ok}
            onPress={() => setModalEndIsVisible(!modalEndIsVisible)}
          />
        </View>
      </Modal>
      <Modal
        visible={modalStartIsVisible}
        onRequestClose={() => setModalStartIsVisible(!modalStartIsVisible)}
        transparent
      >
        <View style={styles.modalView}>
          <TextBlock text={STRINGS.modal_event_creation_failed} bold />
          <TextBlock text={STRINGS.modal_event_starts_in_past} />
          <WideButtonView
            title={STRINGS.modal_button_start_now}
            onPress={() => createElection()}
          />
          <WideButtonView
            title={STRINGS.modal_button_go_back}
            onPress={() => setModalStartIsVisible(!modalStartIsVisible)}
          />
        </View>
      </Modal>
    </ScrollView>
  );
};

export default CreateElection;
