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
import { FIVE_MINUTES_IN_MILLIS } from '../CreateEvent';

const DEFAULT_MEETING_DURATION = 3600;

/**
 * Screen to create a meeting event: a name text input, a start time text and its buttons,
 * a finish time text and its buttons, a location text input, a confirm button and a cancel button
 */

const CreateMeeting = ({ route }: any) => {
  const styles = route.params;

  const navigation = useNavigation();

  const [meetingName, setMeetingName] = useState('');
  const [startTime, setStartTime] = useState(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState(Timestamp.EpochNow().addSeconds(DEFAULT_MEETING_DURATION));

  const [location, setLocation] = useState('');

  const confirmButtonVisibility: boolean = (
    meetingName !== ''
  );

  const createMeeting = () => {
    requestCreateMeeting(meetingName, startTime, location, endTime)
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not create meeting, error:', err);
      });
  };

  const onConfirmPress = () => {
    const now = Timestamp.EpochNow();
    if (endTime.before(now)) {
      // eslint-disable-next-line no-alert
      alert(STRINGS.alert_event_ends_in_past);
    } else if (startTime.before(now.addSeconds(FIVE_MINUTES_IN_MILLIS))) {
      // eslint-disable-next-line no-restricted-globals
      if (confirm(STRINGS.confirm_event_starts_in_past)) {
        createMeeting();
      }
    } else {
      createMeeting();
    }
  };

  const buildDatePickerWeb = () => {
    const newStartTime = new Date(0);
    newStartTime.setUTCSeconds(startTime.valueOf());

    const newEndTime = (endTime.valueOf() !== -1) ? new Date(0) : undefined;
    if (newEndTime !== undefined) {
      newEndTime.setUTCSeconds(endTime.valueOf());
    }

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.meeting_create_start_time} />
          <DatePicker
            selected={newStartTime}
            onChange={(date: Date) => onChangeStartTime(date, setStartTime, setEndTime,
              DEFAULT_MEETING_DURATION)}
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.meeting_create_finish_time} />
          <DatePicker
            selected={newEndTime}
            onChange={(date: Date) => onChangeEndTime(date, startTime, setEndTime)}
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
