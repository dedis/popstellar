import React, { useState } from 'react';
import {
  View, Platform, TextInput,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import DatePicker, { dateToTimestamp } from 'components/DatePicker';
import 'react-datepicker/dist/react-datepicker.css';

import STRINGS from 'res/strings';
import { requestCreateMeeting } from 'network/MessageApi';
import TextBlock from 'components/TextBlock';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import { Timestamp } from 'model/objects';

const ONE_MINUTE = 60;

/**
 * Screen to create a meeting event: a name text input, a start time text and its buttons,
 * a finish time text and its buttons, a location text input, a confirm button and a cancel button
 */
const CreateMeeting = ({ route }: any) => {
  const styles = route.params;

  const navigation = useNavigation();
  const initialStartDate = new Date();
  const initialEndDate = new Date();
  initialEndDate.setHours(initialStartDate.getHours() + 1);

  const [meetingName, setMeetingName] = useState('');
  const [startDate, setStartDate] = useState(dateToTimestamp(initialStartDate));
  const [endDate, setEndDate] = useState(dateToTimestamp(initialEndDate));

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
    const now = new Date();
    if (dateStamp > dateToTimestamp(now)) {
      setStartDate(dateStamp);
      const newEndDate = new Date(date.getTime());
      newEndDate.setHours(date.getHours() + 1);
      setEndDate(dateToTimestamp(newEndDate));
    } else {
      setStartDate(dateToTimestamp(now));
      const newEndDate = new Date(now.getTime());
      newEndDate.setHours(now.getHours() + 1);
      setEndDate(dateToTimestamp(newEndDate));
    }
  };

  const onChangeEndTime = (date: Date) => {
    const dateStamp: Timestamp = dateToTimestamp(date);
    if (dateStamp < startDate) {
      setEndDate(startDate.addSeconds(ONE_MINUTE));
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
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.meeting_create_start_time} />
          <DatePicker
            selected={startTime}
            onChange={(date: Date) => onChangeStartTime(date)}
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.meeting_create_finish_time} />
          <DatePicker
            selected={endTime}
            onChange={(date: Date) => onChangeEndTime(date)}
          />
        </View>
      </View>
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
