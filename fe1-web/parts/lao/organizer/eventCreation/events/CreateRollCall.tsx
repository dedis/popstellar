import React, { useState } from 'react';
import {
  View, Platform, TextInput, ScrollView,
} from 'react-native';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';

import STRINGS from 'res/strings';
import { requestCreateRollCall } from 'network/MessageApi';
import DatePicker from 'components/DatePicker';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import { Timestamp } from 'model/objects';

function dateToTimestamp(date: Date): Timestamp {
  return new Timestamp(Math.floor(date.getTime() / 1000));
}

/**
 * Screen to create a roll-call event
 *
 * FIXME Send the Roll-call event in an open state to the organization server
 *  when the confirm button is press
 */
const CreateRollCall = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();

  const [startDate, setStartDate] = useState(dateToTimestamp(new Date()));

  const [rollCallName, setRollCallName] = useState('');
  const [rollCallLocation, setRollCallLocation] = useState('');
  const [rollCallDescription, setRollCallDescription] = useState('');

  const buildDatePickerWeb = () => (
    <View style={styles.view}>
      <ParagraphBlock text={STRINGS.roll_call_create_deadline} />
      <DatePicker
        selected={startDate}
        onChange={(date: Date) => setStartDate(dateToTimestamp(date))}
      />
    </View>
  );

  const buttonsVisibility: boolean = (rollCallName !== '' && rollCallLocation !== '');

  const onConfirmPress = () => {
    const description = (rollCallDescription === '') ? undefined : rollCallDescription;
    requestCreateRollCall(rollCallName, rollCallLocation, startDate, undefined, description);
    navigation.goBack();
  };

  const onOpenPress = () => {
    console.error('Does nothing for now! See parts/.../CreateRollCall.tsx');
    // requestOpenRollCall(ROLL_CALL_ID, new Timestamp(Math.floor((new Date()).getTime() / 1000)));
    // navigation.goBack();
  };

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
        title={STRINGS.general_button_open}
        onPress={onOpenPress}
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
