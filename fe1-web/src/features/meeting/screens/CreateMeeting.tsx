import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { ConfirmModal, DatePicker, DismissModal, Input } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { onChangeEndTime, onChangeStartTime } from 'core/functions/DatePicker';
import { onConfirmEventCreation } from 'core/functions/UI';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Timestamp } from 'core/objects';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { MeetingHooks } from '../hooks';
import { MeetingFeature } from '../interface';
import { requestCreateMeeting } from '../network/MeetingMessageApi';

const DEFAULT_MEETING_DURATION = 3600;

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.events_create_meeting>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * Screen to create a meeting event: a name text input, a start time text and its buttons,
 * a finish time text and its buttons, a location text input, a confirm button and a cancel button
 */
const CreateMeeting = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();
  const laoId = MeetingHooks.useCurrentLaoId();
  const isConnected = MeetingHooks.useConnectedToLao();

  const [meetingName, setMeetingName] = useState('');
  const [startTime, setStartTime] = useState(Timestamp.EpochNow());
  const [endTime, setEndTime] = useState(Timestamp.EpochNow().addSeconds(DEFAULT_MEETING_DURATION));
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);

  const [location, setLocation] = useState('');

  const confirmButtonEnabled: boolean = isConnected === true && meetingName !== '';

  const createMeeting = () => {
    requestCreateMeeting(laoId, meetingName, startTime, location, endTime)
      .then(() => {
        navigation.navigate(STRINGS.navigation_lao_events_home);
      })
      .catch((err) => {
        console.error('Could not create meeting, error:', err);
        toast.show(`Could not create meeting, error: ${err}`, {
          type: 'danger',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      });
  };

  const buildDatePicker = () => {
    const startDate = startTime.toDate();
    const endDate = endTime.toDate();

    return (
      <>
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.meeting_create_start_time}
        </Text>

        <DatePicker
          value={startDate}
          onChange={(date: Date) =>
            onChangeStartTime(date, setStartTime, setEndTime, DEFAULT_MEETING_DURATION)
          }
        />
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.meeting_create_finish_time}
        </Text>

        <DatePicker
          value={endDate}
          onChange={(date: Date) => onChangeEndTime(date, startTime, setEndTime)}
        />
      </>
    );
  };

  const toolbarItems: ToolbarItem[] = [
    {
      title: STRINGS.meeting_create_meeting,
      disabled: !confirmButtonEnabled,
      onPress: () =>
        onConfirmEventCreation(
          startTime,
          endTime,
          createMeeting,
          setModalStartIsVisible,
          setModalEndIsVisible,
        ),
    },
  ];

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.meeting_create_name}
      </Text>
      <Input
        value={meetingName}
        onChange={setMeetingName}
        placeholder={STRINGS.meeting_create_name_placeholder}
        testID="meeting_name_selector"
      />
      {buildDatePicker()}
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.meeting_create_location}
      </Text>
      <Input
        value={location}
        onChange={setLocation}
        placeholder={STRINGS.meeting_create_location_placeholder}
      />

      {!isConnected && (
        <Text style={[Typography.paragraph, Typography.error]}>
          {STRINGS.event_creation_must_be_connected}
        </Text>
      )}
      {meetingName === '' && (
        <Text style={[Typography.paragraph, Typography.error]}>
          {STRINGS.event_creation_name_not_empty}
        </Text>
      )}

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
      />
    </ScreenWrapper>
  );
};

export default CreateMeeting;

export const CreateMeetingScreen: MeetingFeature.LaoEventScreen = {
  id: STRINGS.events_create_meeting,
  Component: CreateMeeting,
};
