import React, { useState } from 'react';
import {
  View, Platform, Modal,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import DatePicker, { onChangeStartTime, onChangeEndTime } from 'components/DatePicker';
import 'react-datepicker/dist/react-datepicker.css';

import STRINGS from 'res/strings';
import { requestCreateMeeting } from 'network/MessageApi';
import TextBlock from 'components/TextBlock';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import TextInputLine from 'components/TextInputLine';
import { Timestamp } from 'model/objects';
import { onConfirmPress } from '../CreateEvent';

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
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);

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

  const buildDatePickerWeb = () => {
    const startDate = startTime.timestampToDate();
    const endDate = endTime.timestampToDate();

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.meeting_create_start_time} />
          <DatePicker
            selected={startDate}
            onChange={(date: Date) => onChangeStartTime(date, setStartTime, setEndTime,
              DEFAULT_MEETING_DURATION)}
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
        onChangeText={(text: string) => { setMeetingName(text); }}
      />

      { /* see archive branches for date picker used for native apps */ }
      { Platform.OS === 'web' && buildDatePickerWeb() }

      <TextInputLine
        placeholder={STRINGS.meeting_create_location}
        onChangeText={(text: string) => { setLocation(text); }}
      />
      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={() => onConfirmPress(startTime, endTime, createMeeting, setModalStartIsVisible,
          setModalEndIsVisible)}
        disabled={!confirmButtonVisibility}
      />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={navigation.goBack}
      />

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
            onPress={() => createMeeting()}
          />
          <WideButtonView
            title={STRINGS.modal_button_go_back}
            onPress={() => setModalStartIsVisible(!modalStartIsVisible)}
          />
        </View>
      </Modal>
    </>
  );
};

export default CreateMeeting;
