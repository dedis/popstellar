import React, { useState } from 'react';
import { View, ViewStyle, TouchableOpacity } from 'react-native';
import PropTypes from 'prop-types';

import { Spacing } from 'styles/index';
import PROPS_TYPE from 'res/Props';

import ParagraphBlock from 'components/ParagraphBlock';
import styleEventView from 'styles/stylesheets/eventView';
import TextBlock from 'components/TextBlock';
import EventMeeting from './EventMeeting';
import EventRollCall from './EventRollCall';
import ListCollapsibleIcon from '../ListCollapsibleIcon';

/**
 * The Event item component: display the correct representation of the event according to its type,
 * otherwise display its name and in all cases its nested events
*/
const Event = (props: IPropTypes) => {
  const { event } = props;
  const { renderItemFn } = props;
  const isOrganizer = false; // TODO get isOrganizer directly

  const [toggleChildrenVisible, setToggleChildrenVisible] = useState(false);

  const hasChildren = () => event.children && event.children.length !== 0;

  const toggleChildren = () => {
    setToggleChildrenVisible(
      (!!hasChildren() && !toggleChildrenVisible),
    );
  };

  const buildListCollapsibleIcon = () => (
    <TouchableOpacity onPress={toggleChildren} style={{ textAlign: 'right' } as ViewStyle}>
      <ListCollapsibleIcon isOpen={toggleChildrenVisible} />
    </TouchableOpacity>
  );

  const buildEvent = () => {
    switch (event.object) {
      case 'meeting':
        return (
          <EventMeeting
            event={event}
            childrenVisibility={toggleChildrenVisible}
            renderItemFn={renderItemFn}
          />
        );
      case 'roll-call':
        if (isOrganizer) {
          console.log('is organizer => returning null in Event');
          return null;
        }
        return (
          <EventRollCall
            event={event}
            childrenVisibility={toggleChildrenVisible}
            renderItemFn={renderItemFn}
          />
        );
      default:
        return <ParagraphBlock text={`${event.name} (default event => no mapping in Event.tsx)`} />;
    }
  };

  return (
    <View style={[styleEventView.default, { marginTop: Spacing.s }]}>
      <TextBlock text={event.name} />
      { hasChildren() && buildListCollapsibleIcon() }
      { buildEvent() }
    </View>
  );
};

const propTypes = {
  event: PROPS_TYPE.event.isRequired,
  renderItemFn: PropTypes.func.isRequired,
};
Event.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Event;
