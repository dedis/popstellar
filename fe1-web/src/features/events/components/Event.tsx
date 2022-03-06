import React, { useMemo } from 'react';
import { View } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';

import { makeIsLaoOrganizer } from 'features/lao/reducer';
import { Spacing } from 'core/styles';
import { ParagraphBlock, TextBlock } from 'core/components';
import { Hash, Timestamp } from 'core/objects';
import { EventMeeting } from 'features/meeting/components';
import { EventElection } from 'features/evoting/components';
import { EventRollCall } from 'features/rollCall/components';
import { Meeting } from 'features/meeting/objects';
import { Election } from 'features/evoting/objects';
import { RollCall } from 'features/rollCall/objects';

import { OpenedLaoStore } from 'features/lao/store';
import { Lao } from 'features/lao/objects';
import eventViewStyles from '../styles/eventViewStyles';

/**
 * The Event item component: display the correct representation of the event according to its type,
 * otherwise display its name and in all cases its nested events
 */
const Event = (props: IPropTypes) => {
  const { event } = props;

  const isOrganizerSelect = useMemo(makeIsLaoOrganizer, []);
  const isOrganizer = useSelector(isOrganizerSelect);
  const currentLao: Lao = OpenedLaoStore.get();

  const buildEvent = () => {
    if (event instanceof Meeting) {
      return <EventMeeting event={event} />;
    }
    if (event instanceof RollCall) {
      return <EventRollCall event={event} isOrganizer={isOrganizer} />;
    }
    if (event instanceof Election) {
      return <EventElection laoId={currentLao.id} election={event} isOrganizer={isOrganizer} />;
    }
    return <ParagraphBlock text={`${event.name} (default event => no mapping in Event.tsx)`} />;
  };

  return (
    <View style={[eventViewStyles.default, { marginTop: Spacing.s }]}>
      <TextBlock text={event.name} />
      {buildEvent()}
    </View>
  );
};

const propTypes = {
  event: PropTypes.shape({
    id: PropTypes.instanceOf(Hash).isRequired,
    name: PropTypes.string.isRequired,
    start: PropTypes.instanceOf(Timestamp).isRequired,
    end: PropTypes.instanceOf(Timestamp),
  }).isRequired,
};
Event.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export const eventPropTypes = propTypes.event;
export default Event;
