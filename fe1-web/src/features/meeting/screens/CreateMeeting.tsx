import React, { useState } from 'react';
import { View, Platform } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import 'react-datepicker/dist/react-datepicker.css';
import { useToast } from 'react-native-toast-notifications';

import STRINGS from 'resources/strings';
import {
  ConfirmModal,
  DatePicker,
  DismissModal,
  ParagraphBlock,
  TextBlock,
  TextInputLine,
  WideButtonView,
} from 'core/components';
import { onChangeStartTime, onChangeEndTime } from 'core/components/DatePicker';
import { Timestamp } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';
import { onConfirmPress } from 'features/events/screens/CreateEvent';

import { requestCreateMeeting } from '../network/MeetingMessageApi';

const DEFAULT_MEETING_DURATION = 3600;

/**
 * Screen to create a meeting event: a name text input, a start time text and its buttons,
 * a finish time text and its buttons, a location text input, a confirm button and a cancel button
 */

const CreateMeeting = ({ route }: any) => {
  const styles = route.params;

  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();
  const toast = useToast();

  const [meetingName, setMeetingName] = useState('');
  const [startTime, setStartTime] = useState(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState(Timestamp.EpochNow().addSeconds(DEFAULT_MEETING_DURATION));
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);

  const [location, setLocation] = useState('');

  const confirmButtonVisibility: boolean = meetingName !== '';

  const createMeeting = () => {
    requestCreateMeeting(meetingName, startTime, location, endTime)
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not create meeting, error:', err);
        toast.show(`Could not create meeting, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const buildDatePickerWeb = () => {
    const startDate = startTime.toDate();
    const endDate = endTime.toDate();

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.meeting_create_start_time} />
          <DatePicker
            selected={startDate}
            onChange={(date: Date) =>
              onChangeStartTime(date, setStartTime, setEndTime, DEFAULT_MEETING_DURATION)
            }
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.meeting_create_finish_time} />
          <DatePicker
            selected={endDate}
            onChange={(date: Date) => onChangeEndTime(date, startTime, setEndTime)}
          />
        </View>
      </View>
    );
  };

  return (
    <>
      <TextBlock text="Create a meeting" />
      <TextInputLine
        placeholder={STRINGS.meeting_create_name}
        onChangeText={(text: string) => {
          setMeetingName(text);
        }}
      />

      {/* see archive branches for date picker used for native apps */}
      {Platform.OS === 'web' && buildDatePickerWeb()}

      <TextInputLine
        placeholder={STRINGS.meeting_create_location}
        onChangeText={(text: string) => {
          setLocation(text);
        }}
      />
      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={() =>
          onConfirmPress(
            startTime,
            endTime,
            createMeeting,
            setModalStartIsVisible,
            setModalEndIsVisible,
          )
        }
        disabled={!confirmButtonVisibility}
      />
      <WideButtonView title={STRINGS.general_button_cancel} onPress={navigation.goBack} />

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
        onConfirmPress={() => createMeeting()}
        buttonConfirmText={STRINGS.modal_button_start_now}
        buttonCancelText={STRINGS.modal_button_go_back}
      />
    </>
  );
};

export default CreateMeeting;
