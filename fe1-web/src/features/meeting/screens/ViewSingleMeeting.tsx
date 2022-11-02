import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import { ParagraphBlock, TimeDisplay } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
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

  const selectMeeting = useMemo(() => makeMeetingSelector(meetingId), [meetingId]);
  const meeting = useSelector(selectMeeting);

  if (!meeting) {
    throw new Error(`Could not find a meeting with id ${meetingId}`);
  }

  return (
    <ScreenWrapper>
      <TimeDisplay start={meeting.start.valueOf()} />
      {meeting.end && <TimeDisplay end={meeting.end.valueOf()} />}
      {meeting.location && <ParagraphBlock text={meeting.location} />}
    </ScreenWrapper>
  );
};

export default ViewSingleMeeting;

/**
 * Component rendered in the top middle of the navgiation bar when looking
 * at a single meeting. Makes sure it shows the name of the meeting and
 * not just some static string.
 */
export const ViewSingleMeetingScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: meetingId } = route.params;

  const selectMeeting = useMemo(() => makeMeetingSelector(meetingId), [meetingId]);
  const meeting = useSelector(selectMeeting);

  if (!meeting) {
    throw new Error(`Could not find a meeting with id ${meetingId}`);
  }

  return (
    <Text style={Typography.topNavigationHeading} numberOfLines={1}>
      {meeting.name}
    </Text>
  );
};

export const ViewSingleMeetingScreen: MeetingFeature.LaoEventScreen = {
  id: STRINGS.events_view_single_meeting,
  Component: ViewSingleMeeting,
  headerTitle: ViewSingleMeetingScreenHeader,
};
