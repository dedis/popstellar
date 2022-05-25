import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { ListItem } from 'react-native-elements';
import { useSelector } from 'react-redux';

import ElectionIcon from 'core/components/icons/ElectionIcon';
import { Color, Icon, Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { EvotingInterface } from '../interface';
import { Election, ElectionStatus } from '../objects';
import { makeElectionSelector } from '../reducer';

const styles = StyleSheet.create({
  icon: {
    marginRight: Spacing.x1,
  } as ViewStyle,
});

const getSubtitle = (election: Election): string => {
  if (election.electionStatus === ElectionStatus.NOT_STARTED) {
    return `${STRINGS.general_starting_at} ${election.start
      .toDate()
      .toLocaleDateString()} ${election.start.toDate().toLocaleTimeString()}`;
  }

  if (election.electionStatus === ElectionStatus.OPENED) {
    return `${STRINGS.general_ongoing}, ${STRINGS.general_ending_at} ${election.end
      .toDate()
      .toLocaleDateString()} ${election.end.toDate().toLocaleTimeString()}`;
  }

  return `${STRINGS.general_ended_at} ${election.end.toDate().toLocaleDateString()} ${election.end
    .toDate()
    .toLocaleTimeString()}`;
};

const ElectionListItem = (props: IPropTypes) => {
  const { eventId: electionId } = props;

  const selectElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectElection);

  if (!election) {
    throw new Error(`Could not find an election with id ${electionId}`);
  }

  return (
    <>
      <View style={styles.icon}>
        <ElectionIcon color={Color.primary} size={Icon.size} />
      </View>
      <ListItem.Content>
        <ListItem.Title>{election.name}</ListItem.Title>
        <ListItem.Subtitle>{getSubtitle(election)}</ListItem.Subtitle>
      </ListItem.Content>
      <ListItem.Chevron />
    </>
  );
};

const propTypes = {
  eventId: PropTypes.string.isRequired,
};
ElectionListItem.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionListItem;

export const ElectionEventType: EvotingInterface['eventTypes']['0'] = {
  eventType: Election.EVENT_TYPE,
  eventName: STRINGS.election_event_name,
  navigationNames: {
    createEvent: STRINGS.navigation_lao_events_create_election,
    screenSingle: STRINGS.navigation_lao_events_view_single_election,
  },
  ListItemComponent: ElectionListItem as FunctionComponent<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>,
};
