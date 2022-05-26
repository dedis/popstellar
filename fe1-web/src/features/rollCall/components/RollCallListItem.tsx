import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo } from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { useSelector } from 'react-redux';

import RollCallIcon from 'core/components/icons/RollCallIcon';
import { Color, Icon, List } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCallInterface } from '../interface';
import { RollCall, RollCallStatus } from '../objects';
import { makeRollCallSelector } from '../reducer';

const getSubtitle = (rollCall: RollCall): string => {
  if (rollCall.status === RollCallStatus.CREATED) {
    return `${STRINGS.general_starting_at} ${rollCall.start
      .toDate()
      .toLocaleDateString()} ${rollCall.start.toDate().toLocaleTimeString()}, ${rollCall.location}`;
  }

  if (rollCall.status === RollCallStatus.OPENED || rollCall.status === RollCallStatus.REOPENED) {
    return `${STRINGS.general_ongoing}, ${rollCall.location}`;
  }

  return `${STRINGS.general_ended_at} ${rollCall.end.toDate().toLocaleDateString()} ${rollCall.end
    .toDate()
    .toLocaleTimeString()}, ${rollCall.location}`;
};

const RollCallListItem = (props: IPropTypes) => {
  const { eventId: rollCallId } = props;

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);

  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  return (
    <>
      <View style={List.icon}>
        <RollCallIcon color={Color.primary} size={Icon.size} />
      </View>
      <ListItem.Content>
        <ListItem.Title>{rollCall.name}</ListItem.Title>
        <ListItem.Subtitle>{getSubtitle(rollCall)}</ListItem.Subtitle>
      </ListItem.Content>
      <ListItem.Chevron />
    </>
  );
};

const propTypes = {
  eventId: PropTypes.string.isRequired,
};
RollCallListItem.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallListItem;

export const RollCallEventType: RollCallInterface['eventTypes']['0'] = {
  eventType: RollCall.EVENT_TYPE,
  eventName: STRINGS.roll_call_event_name,
  navigationNames: {
    createEvent: STRINGS.navigation_lao_events_create_roll_call,
    screenSingle: STRINGS.navigation_lao_events_view_single_roll_call,
  },
  ListItemComponent: RollCallListItem as FunctionComponent<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>,
};
