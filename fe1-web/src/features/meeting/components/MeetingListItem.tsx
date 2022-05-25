import PropTypes from 'prop-types';
import React, { useMemo } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { ListItem } from 'react-native-elements';
import { useSelector } from 'react-redux';

import MeetingIcon from 'core/components/icons/MeetingIcon';
import { Timestamp } from 'core/objects';
import { Color, Icon, Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { MeetingInterface } from '../interface';
import { Meeting } from '../objects';
import { makeMeetingSelector } from '../reducer';

const styles = StyleSheet.create({
  icon: {
    marginRight: Spacing.x1,
  } as ViewStyle,
});

const getSubtitle = (meeting: Meeting): string => {
  const now = Timestamp.EpochNow();

  if (meeting.start.after(now)) {
    return `${STRINGS.general_starting_at} ${meeting.start
      .toDate()
      .toLocaleDateString()} ${meeting.start.toDate().toLocaleTimeString()}, ${meeting.location}`;
  }

  if (!meeting.end || meeting.end.after(now)) {
    return `${STRINGS.general_ongoing}, ${meeting.location}`;
  }

  return `${STRINGS.general_ended_at} ${meeting.end.toDate().toLocaleDateString()} ${meeting.end
    .toDate()
    .toLocaleTimeString()}, ${meeting.location}`;
};

const MeetingListItem = (props: IPropTypes) => {
  const { eventId: meetingId } = props;

  const selectMeeting = useMemo(() => makeMeetingSelector(meetingId), [meetingId]);
  const meeting = useSelector(selectMeeting);

  if (!meeting) {
    throw new Error(`Could not find a meeting with id ${meetingId}`);
  }

  return (
    <>
      <View style={styles.icon}>
        <MeetingIcon color={Color.primary} size={Icon.size} />
      </View>
      <ListItem.Content>
        <ListItem.Title>{meeting.name}</ListItem.Title>
        <ListItem.Subtitle>{getSubtitle(meeting)}</ListItem.Subtitle>
      </ListItem.Content>
      <ListItem.Chevron />
    </>
  );
};

const propTypes = {
  eventId: PropTypes.string.isRequired,
};
MeetingListItem.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default MeetingListItem;

export const MeetingEventType: MeetingInterface['eventTypes']['0'] = {
  eventType: Meeting.EVENT_TYPE,
  eventName: STRINGS.meeting_event_name,
  navigationNames: {
    createEvent: STRINGS.navigation_lao_events_create_meeting,
    screenSingle: STRINGS.navigation_lao_events_view_single_meeting,
  },
  ListItemComponent: MeetingListItem as React.FunctionComponent<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>,
};
