import React, { useState } from 'react';
import {
  View, Platform, TextInput, ScrollView,
} from 'react-native';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';

import STRINGS from 'res/strings';
import { requestCreateRollCall } from 'network/MessageApi';
import DatePicker, { onChangeStartTime, onChangeEndTime } from 'components/DatePicker';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import { Timestamp } from 'model/objects';

const DEFAULT_ROLL_CALL_DURATION = 3600;

/**
 * Screen to create a roll-call event
 *
 * TODO Send the Roll-call event in an open state to the organization server
 *  when the confirm button is press
 */
const CreateRollCall = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();

  const [proposedStartDate, setProposedStartDate] = useState(Timestamp.EpochNow());
  const [proposedEndDate, setProposedEndDate] = useState(Timestamp.EpochNow()
    .addSeconds(DEFAULT_ROLL_CALL_DURATION));

  const [rollCallName, setRollCallName] = useState('');
  const [rollCallLocation, setRollCallLocation] = useState('');
  const [rollCallDescription, setRollCallDescription] = useState('');

  const buildDatePickerWeb = () => {
    const startTime = new Date(0);
    const endTime = new Date(0);
    startTime.setUTCSeconds(proposedStartDate.valueOf());
    endTime.setUTCSeconds(proposedEndDate.valueOf());

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.roll_call_create_proposed_start} />
          <DatePicker
            selected={startTime}
            onChange={(date: Date) => onChangeStartTime(date, setProposedStartDate,
              setProposedEndDate, DEFAULT_ROLL_CALL_DURATION)}
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.roll_call_create_proposed_end} />
          <DatePicker
            selected={endTime}
            onChange={(date: Date) => onChangeEndTime(date, proposedStartDate, setProposedEndDate)}
          />
        </View>
      </View>
    );
  };

  const buttonsVisibility: boolean = (rollCallName !== '' && rollCallLocation !== '');

  const createRollCall = () => {
    const description = (rollCallDescription === '') ? undefined : rollCallDescription;
    requestCreateRollCall(
      rollCallName, rollCallLocation, proposedStartDate, proposedEndDate,
      description,
    )
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not create roll call, error:', err);
      });
  };

  const onConfirmPress = () => createRollCall();

  return (
    <ScrollView>
      { /* see archive branches for date picker used for native apps */ }
      { Platform.OS === 'web' && buildDatePickerWeb() }

      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.roll_call_create_name}
        onChangeText={(text: string) => { setRollCallName(text); }}
      />
      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.roll_call_create_location}
        onChangeText={(text: string) => { setRollCallLocation(text); }}
      />
      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.roll_call_create_description}
        onChangeText={(text: string) => { setRollCallDescription(text); }}
      />

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

export default CreateRollCall;
