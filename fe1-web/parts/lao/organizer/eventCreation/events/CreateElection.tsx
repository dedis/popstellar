import React, { useState } from 'react';
import {
  View, Platform, TextInput, ScrollView,
} from 'react-native';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';

import STRINGS from 'res/strings';
import DatePicker from 'components/DatePicker';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import { Timestamp } from 'model/objects';
import TextBlock from 'components/TextBlock';
import DropdownSelector from 'components/DropdownSelector';
import TextInputList from 'components/TextInputList';

/**
 * UI to create a Election Event
 *
 * TODO Send the Election event to the organization server when the confirm button is pressed
 */
const CreateElection = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();
  const initialStartDate = new Date();
  const initialEndDate = new Date();
  // Sets initial end date to 1 hour later than start date
  initialEndDate.setHours(initialEndDate.getHours() + 1);

  const [startDate, setStartDate] = useState(Timestamp.dateToTimestamp(initialStartDate));
  const [endDate, setEndDate] = useState(Timestamp.dateToTimestamp(initialEndDate));

  const [electionName, setElectionName] = useState('');
  const [electionQuestion, setElectionQuestion] = useState('');
  const [electionBallots, setElectionBallots] = useState(['']);
  const votingMethods = [STRINGS.election_method_Plurality];
  const [votingMethod, setVotingMethod] = useState(votingMethods[0]);
  const minBallotOptions = 2;

  const buildDatePickerWeb = () => {
    const startTime = new Date(0);
    const endTime = new Date(1);
    startTime.setUTCSeconds(startDate.valueOf());
    endTime.setUTCSeconds(endDate.valueOf());

    return (
      <View style={styles.view}>
        <ParagraphBlock text={STRINGS.election_create_start_time} />
        <DatePicker
          selected={startTime}
          onChange={(date: Date) => setStartDate(Timestamp.dateToTimestamp(date))}
        />
        <ParagraphBlock text={STRINGS.election_create_finish_time} />
        <DatePicker
          selected={endTime}
          onChange={(date: Date) => setEndDate(Timestamp.dateToTimestamp(date))}
        />
      </View>
    );
  };

  // Confirm button only clickable when the Name, Question and 2 Ballot options have values
  const buttonsVisibility: boolean = (electionQuestion !== '' && electionName !== ''
    && electionBallots.length >= minBallotOptions);

  const onConfirmPress = () => {
    // Todo: Make this button send the request to create this election and remove console logs
    console.log('Election name: ', electionName);
    console.log('Start Date: ', startDate);
    console.log('End Date: ', endDate);
    console.log('Question: ', electionQuestion);
    console.log('Voting Method: ', votingMethod);
    console.log('Ballots: ', electionBallots);
  };

  return (
    <ScrollView>
      <TextBlock text={STRINGS.election_create_setup} bold />
      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.election_create_name}
        onChangeText={(text: string) => { setElectionName(text); }}
      />
      { /* see archive branches for date picker used for native apps */ }
      { Platform.OS === 'web' && buildDatePickerWeb() }
      <TextBlock text={STRINGS.election_voting_method} />
      <DropdownSelector
        values={votingMethods}
        onChange={(method: string) => setVotingMethod(method)}
      />
      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.election_create_question}
        onChangeText={(text: string) => { setElectionQuestion(text); }}
      />
      <TextBlock text={STRINGS.election_create_ballot_options} />
      <TextInputList onChange={setElectionBallots} />
      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={onConfirmPress}
        disabled={!buttonsVisibility}
      />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={navigation.goBack}
      />
    </ScrollView>
  );
};

export default CreateElection;
