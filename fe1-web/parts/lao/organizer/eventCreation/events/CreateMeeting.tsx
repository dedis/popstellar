import React, { useState } from 'react';
import {
  View, Button, Platform, TextInput,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import DatePicker from 'components/DatePicker';
import 'react-datepicker/dist/react-datepicker.css';

import STRINGS from 'res/strings';
import { requestCreateMeeting } from 'network/MessageApi';
import TextBlock from 'components/TextBlock';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import { Timestamp } from 'model/objects';

/**
 * Screen to create a meeting event: a name text input, a start time text and its buttons,
 * a finish time text and its buttons, a location text input, a confirm button and a cancel button
 *
 * TODO makes impossible to set a finish time before the start time
 */
function dateToTimestamp(date: Date): Timestamp {
  return new Timestamp(Math.floor(date.getTime() / 1000));
}

const CreateMeeting = ({ route }: any) => {
  const styles = route.params;

  const navigation = useNavigation();
  const initialStartDate = new Date();

  const [meetingName, setMeetingName] = useState('');
  const [startDate, setStartDate] = useState(dateToTimestamp(initialStartDate));
  const [endDate, setEndDate] = useState(new Timestamp(-1));

  const [location, setLocation] = useState('');

  const confirmButtonVisibility: boolean = (
    meetingName !== ''
    && Math.floor(startDate.valueOf() / 60) >= Math.floor((new Date()).getTime() / 60000)
  );

  const onConfirmPress = () => {
    const endTime = (endDate.valueOf() === -1) ? undefined : endDate;

    requestCreateMeeting(meetingName, startDate, location || undefined, endTime)
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not create meeting, error:', err);
      });
  };

  const onChangeStartTime = (date: Date) => {
    const dateStamp: Timestamp = dateToTimestamp(date);
    setStartDate(dateStamp);
    if (endDate < startDate) {
      setEndDate(dateStamp);
    }
  };

  const onChangeEndTime = (date: Date) => {
    const dateStamp: Timestamp = dateToTimestamp(date);

    if (dateStamp < startDate) {
      setEndDate(startDate);
    } else {
      setEndDate(dateStamp);
    }
  };

  const buildDatePickerWeb = () => {
    const startTime = new Date(0);
    startTime.setUTCSeconds(startDate.valueOf());

    const endTime = (endDate.valueOf() !== -1) ? new Date(0) : undefined;
    if (endTime !== undefined) {
      endTime.setUTCSeconds(endDate.valueOf());
    }

    return (
      <>
        { /* Start time */ }
        <View style={styles.view}>
          <ParagraphBlock text={STRINGS.meeting_create_start_time} />
          { /* zIndexBooster corrects the problem of DatePicker being other elements */ }
          <DatePicker selected={startTime} onChange={onChangeStartTime} />
        </View>

        { /* End time */}
        <View style={styles.view}>
          <ParagraphBlock text={STRINGS.meeting_create_finish_time} />
          { /* zIndexBooster corrects the problem of DatePicker being other elements */}
          <DatePicker selected={endTime} onChange={onChangeEndTime} />
          <View>
            { /* the view is there to avoid button stretching */ }
            <Button onPress={() => setEndDate(new Timestamp(-1))} title="Clear" />
          </View>
        </View>
      </>
    );
  };

  return (
    <>
      <TextBlock text="Create a meeting" />
      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.meeting_create_name}
        onChangeText={(text: string) => { setMeetingName(text); }}
      />

      { /* see archive branches for date picker used for native apps */ }
      { Platform.OS === 'web' && buildDatePickerWeb() }

      <TextInput
        style={styles.textInput}
        placeholder={STRINGS.meeting_create_location}
        onChangeText={(text: string) => { setLocation(text); }}
      />
      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={onConfirmPress}
        disabled={!confirmButtonVisibility}
      />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={navigation.goBack}
      />
    </>
  );
};

export default CreateMeeting;
