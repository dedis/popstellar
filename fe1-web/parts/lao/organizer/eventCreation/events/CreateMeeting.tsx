import React, { useState } from 'react';
import {
  View, Platform, TextInput,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import DatePicker, { onChangeStartTime, onChangeEndTime } from 'components/DatePicker';
import 'react-datepicker/dist/react-datepicker.css';

import STRINGS from 'res/strings';
import { requestCreateMeeting } from 'network/MessageApi';
import TextBlock from 'components/TextBlock';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import { Timestamp } from 'model/objects';
import { ONE_HOUR_IN_SECONDS } from './CreateElection';

/**
 * Screen to create a meeting event: a name text input, a start time text and its buttons,
 * a finish time text and its buttons, a location text input, a confirm button and a cancel button
 */
const CreateMeeting = ({ route }: any) => {
  const styles = route.params;

  const navigation = useNavigation();

  const [meetingName, setMeetingName] = useState('');
  const [startDate, setStartDate] = useState(Timestamp.EpochNow());
  const [endDate, setEndDate] = useState(Timestamp.EpochNow().addSeconds(ONE_HOUR_IN_SECONDS));

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
            onChange={(date: Date) => onChangeStartTime(date, setStartDate, setEndDate)}
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.meeting_create_finish_time} />
          <DatePicker
            selected={endTime}
            onChange={(date: Date) => onChangeEndTime(date, startDate, setEndDate)}
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
