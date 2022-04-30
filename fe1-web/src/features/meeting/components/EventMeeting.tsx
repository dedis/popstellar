import PropTypes from 'prop-types';
import React, { FunctionComponent } from 'react';

import { ParagraphBlock, TimeDisplay } from 'core/components';

import { Meeting } from '../objects';

/**
 * Component used to display a Meeting event in the LAO event list
 */

const EventMeeting = (props: IPropTypes) => {
  const { event } = props;

  return (
    <>
      <TimeDisplay start={event.start.valueOf()} />
      {event.end && <TimeDisplay end={event.end.valueOf()} />}
      {event.location && <ParagraphBlock text={event.location} />}
    </>
  );
};

const propTypes = {
  event: PropTypes.instanceOf(Meeting).isRequired,
};
EventMeeting.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventMeeting;

export const MeetingEventTypeComponent = {
  isOfType: (event: unknown) => event instanceof Meeting,
  Component: EventMeeting as FunctionComponent<{
    event: unknown;
    isOrganizer: boolean | null | undefined;
  }>,
};
