import React, { useMemo } from 'react';
import { View } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';

import { selectIsLaoOrganizer } from 'features/lao/reducer';
import { Spacing } from 'core/styles';
import { ParagraphBlock, TextBlock } from 'core/components';
import { Hash, Timestamp } from 'core/objects';

import eventViewStyles from '../styles/eventViewStyles';
import { EventsHooks } from '../hooks';

/**
 * The Event item component: display the correct representation of the event according to its type,
 * otherwise display its name and in all cases its nested events
 */
const Event = (props: IPropTypes) => {
  const { event } = props;

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const eventTypeComponents = EventsHooks.useEventTypeComponents();

  const Component = useMemo(() => {
    return eventTypeComponents.find((c) => c.isOfType(event))?.Component;
  }, [event, eventTypeComponents]);

  return (
    <View style={[eventViewStyles.default, { marginTop: Spacing.s }]}>
      <TextBlock text={event.name} />
      {Component ? (
        <Component event={event} isOrganizer={isOrganizer} />
      ) : (
        <ParagraphBlock text={`${event.name} (default event => no mapping in Event.tsx)`} />
      )}
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
