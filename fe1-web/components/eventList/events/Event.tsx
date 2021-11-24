import React from 'react';
import { View } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { makeIsLaoOrganizer } from 'store';

import { Spacing } from 'styles';

import ParagraphBlock from 'components/ParagraphBlock';
import styleEventView from 'styles/stylesheets/eventView';
import TextBlock from 'components/TextBlock';
import {
  Election, Hash, RollCall, Timestamp, Meeting,
} from 'model/objects';
import EventMeeting from './EventMeeting';
import EventRollCall from './EventRollCall';
import EventElection from './EventElection';

/**
 * The Event item component: display the correct representation of the event according to its type,
 * otherwise display its name and in all cases its nested events
*/
const Event = (props: IPropTypes) => {
  const { event } = props;

  const isOrganizerSelect = makeIsLaoOrganizer();
  const isOrganizer = useSelector(isOrganizerSelect);

  const buildEvent = () => {
    if (event instanceof Meeting) {
      return (
        <EventMeeting
          event={event}
        />
      );
    }
    if (event instanceof RollCall) {
      return (
        <EventRollCall
          event={event}
          isOrganizer={isOrganizer}
        />
      );
    }
    if (event instanceof Election) {
      return (
        <EventElection
          election={event}
          isOrganizer={isOrganizer}
        />
      );
    }
    return <ParagraphBlock text={`${event.name} (default event => no mapping in Event.tsx)`} />;
  };

  return (
    <View style={[styleEventView.default, { marginTop: Spacing.s }]}>
      <TextBlock text={event.name} />
      { buildEvent() }
    </View>
  );
};

const propTypes = {
  event: PropTypes.shape({
    id: PropTypes.instanceOf(Hash).isRequired,
    start: PropTypes.instanceOf(Timestamp).isRequired,
    end: PropTypes.instanceOf(Timestamp),
  }).isRequired,
};
Event.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Event;
