import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import { TimeDisplay } from 'core/components';
import DateRange from 'core/components/DateRange';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Hash } from 'core/objects';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { MeetingFeature } from '../interface';
import { makeMeetingSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.events_view_single_meeting>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const ViewSingleMeeting = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: meetingId } = route.params;

  const selectMeeting = useMemo(() => makeMeetingSelector(new Hash(meetingId)), [meetingId]);
  const meeting = useSelector(selectMeeting);

  if (!meeting) {
    throw new Error(`Could not find a meeting with id ${meetingId}`);
  }

  return (
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{meeting.name}</Text>
        {meeting.location && (
          <>
            {'\n'}
            <Text>{meeting.location}</Text>
          </>
        )}
      </Text>

      <Text style={Typography.paragraph}>
        {meeting.end ? (
          <DateRange start={meeting.start.toDate()} end={meeting.end.toDate()} />
        ) : (
          <TimeDisplay start={meeting.start.valueOf()} />
        )}
      </Text>
    </ScreenWrapper>
  );
};

export default ViewSingleMeeting;

export const ViewSingleMeetingScreen: MeetingFeature.LaoEventScreen = {
  id: STRINGS.events_view_single_meeting,
  Component: ViewSingleMeeting,
  headerTitle: STRINGS.meeting_event_name,
};
