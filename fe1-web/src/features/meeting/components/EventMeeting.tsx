import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo } from 'react';
import { useSelector } from 'react-redux';

import { ParagraphBlock, TimeDisplay } from 'core/components';
import STRINGS from 'resources/strings';

import { Meeting } from '../objects';
import { makeMeetingSelector } from '../reducer';

/**
 * Component used to display a Meeting event in the LAO event list
 */

const EventMeeting = (props: IPropTypes) => {
  const { eventId: meetingId } = props;

  const selectMeeting = useMemo(() => makeMeetingSelector(meetingId), [meetingId]);
  const meeting = useSelector(selectMeeting);

  if (!meeting) {
    throw new Error(`Could not find a meeting with id ${meetingId}`);
  }

  return (
    <>
      <TimeDisplay start={meeting.start.valueOf()} />
      {meeting.end && <TimeDisplay end={meeting.end.valueOf()} />}
      {meeting.location && <ParagraphBlock text={meeting.location} />}
    </>
  );
};

const propTypes = {
  eventId: PropTypes.string.isRequired,
};
EventMeeting.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventMeeting;

export const MeetingEventType = {
  eventType: Meeting.EVENT_TYPE,
  navigationNames: {
    createEvent: STRINGS.organizer_navigation_creation_meeting,
  },
  Component: EventMeeting as FunctionComponent<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>,
};
