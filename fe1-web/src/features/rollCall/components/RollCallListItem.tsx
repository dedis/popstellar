import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo } from 'react';
import { View } from 'react-native';
import { useSelector } from 'react-redux';
import ReactTimeago from 'react-timeago';

import { PoPIcon } from 'core/components';
import { Color, Icon, List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCallInterface } from '../interface';
import { RollCall, RollCallStatus } from '../objects';
import { makeRollCallSelector } from '../reducer';

const Subtitle = ({ rollCall }: { rollCall: RollCall }) => {
  if (rollCall.status === RollCallStatus.CREATED) {
    return (
      <>
        {STRINGS.general_starting_at} <ReactTimeago date={rollCall.start.toDate()} />,{' '}
        {rollCall.location}
      </>
    );
  }

  if (rollCall.status === RollCallStatus.OPENED || rollCall.status === RollCallStatus.REOPENED) {
    return (
      <>
        {STRINGS.general_ongoing}, {rollCall.location}
      </>
    );
  }

  return (
    <>
      {STRINGS.general_closed} <ReactTimeago date={rollCall.end.toDate()} />
    </>
  );
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
        <PoPIcon name="rollCall" color={Color.primary} size={Icon.size} />
      </View>
      <ListItem.Content>
        <ListItem.Title style={Typography.base}>{rollCall.name}</ListItem.Title>
        <ListItem.Subtitle style={Typography.small}>
          <Subtitle rollCall={rollCall} />
        </ListItem.Subtitle>
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

export const RollCallEventType: RollCallInterface['eventTypes'][0] = {
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
