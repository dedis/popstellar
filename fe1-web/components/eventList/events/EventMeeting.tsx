import React from 'react';
import { FlatList } from 'react-native';

import PROPS_TYPE from 'res/Props';
import ParagraphBlock from 'components/ParagraphBlock';
import PropTypes from 'prop-types';

/**
 * Component used to display a Meeting event in the LAO event list
 */

const dateToString = (timestamp: number) => ((new Date(timestamp * 1000)).toLocaleString());

const EventMeeting = (props: IPropTypes) => {
  const { event } = props;
  const { childrenVisibility } = props;
  const { renderItemFn } = props;

  return (
    <>
      <ParagraphBlock text={`Starts at ${dateToString(event.start)}`} />

      { event.end && <ParagraphBlock text={`Ends  at ${dateToString(event.end)}`} /> }
      { event.location && <ParagraphBlock text={event.location} /> }
      { childrenVisibility && (
        <FlatList
          data={event.children}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderItemFn}
          listKey={`MeetingEvent-${event.id.toString()}`}
        />
      )}
    </>
  );
};

const propTypes = {
  event: PROPS_TYPE.meeting.isRequired,
  childrenVisibility: PropTypes.bool.isRequired,
  renderItemFn: PropTypes.func.isRequired,
};
EventMeeting.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventMeeting;
