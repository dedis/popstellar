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

function dateToTimestamp(date: Date): Timestamp {
  return new Timestamp(Math.floor(date.getTime() / 1000));
}

/**
 * Screen to create a Election Event
 *
 * TODO Send the Election event in an open state to the organization server
 *  when the confirm button is pressed
 */
const CreateElection = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();
  const initialStartDate = new Date();
  const initialEndDate = new Date();
  // Sets initial end date to 1 hour later than start date
  initialEndDate.setHours(initialEndDate.getHours() + 1);

  const [startDate, setStartDate] = useState(dateToTimestamp(initialStartDate));
  const [endDate, setEndDate] = useState(dateToTimestamp(initialEndDate));

  const [electionName, setElectionName] = useState('');
  const [electionQuestion, setElectionQuestion] = useState('');
  const [electionBallots, setElectionBallots] = useState(['']);
  const [electionBallotCounter, setElectionBallotCounter] = useState(2);
  const [selectedElectionMethod, setSelectedElectionMethod] = useState('');
  const ballotOptionsUIComponents = [];

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
          onChange={(date: Date) => setStartDate(dateToTimestamp(date))}
        />
        <ParagraphBlock text={STRINGS.election_create_finish_time} />
        <DatePicker
          selected={endTime}
          onChange={(date: Date) => setEndDate(dateToTimestamp(date))}
        />
      </View>
    );
  };

  const buttonsVisibility: boolean = (electionBallots.length >= 2
    && electionQuestion !== '' && electionName !== '');

  // Makes sure you can't remove ballots when there are only 2 options
  const removeButtonVisibility: boolean = (electionBallotCounter > 2);

  const onAddBallotPress = () => {
    setElectionBallotCounter((prevCount) => prevCount + 1);
  };

  const onRemoveBallotPress = () => {
    // decrements counter
    setElectionBallotCounter((prevCount) => prevCount - 1);
    // removes value from ballot array
    const newArr = [...electionBallots];
    newArr.splice(electionBallotCounter - 1, 1);
    setElectionBallots(newArr);
  };

  // Updates the array with the specified ballot entries in the textfields
  const updateBallotArray = (index: number, text: string) => {
    const newArr = [...electionBallots];
    newArr[index] = text;
    setElectionBallots(newArr);
  };

  // Creates all the Ballot option text-fields
  for (let i = 0; i < electionBallotCounter; i += 1) {
    const ballotPlaceholder = `${STRINGS.election_create_ballot_option} ${i + 1}`;
    ballotOptionsUIComponents.push(<TextInput
      style={styles.textInput}
      placeholder={ballotPlaceholder} // Add index in the setState
      onChangeText={(text: string) => { updateBallotArray(i, text); }}
    />);
  }

  const onConfirmPress = () => {
    // const description = (rollCallDescription === '') ? undefined : rollCallDescription;
    // requestCreateRollCall(electionName, electionQuestion, startDate, undefined, description);

    navigation.goBack();
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
      <DropdownSelector
        values={[STRINGS.election_method_Plurality]}
        onChange={(method: string) => setSelectedElectionMethod(method)}
      />

      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.election_create_question}
        onChangeText={(text: string) => { setElectionQuestion(text); }}
      />
      <TextBlock text={STRINGS.election_create_ballot_options} />
      <WideButtonView
        onPress={onAddBallotPress}
        title="+"
      />
      <WideButtonView
        onPress={onRemoveBallotPress}
        disabled={!removeButtonVisibility}
        title="-"
      />
      {ballotOptionsUIComponents}
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
