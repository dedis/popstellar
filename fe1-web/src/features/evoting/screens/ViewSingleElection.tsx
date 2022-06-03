import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import ElectionNotStarted, {
  ElectionNotStartedRightHeader,
} from '../components/ElectionNotStarted';
import ElectionOpened, { ElectionOpenedRightHeader } from '../components/ElectionOpened';
import ElectionResult from '../components/ElectionResult';
import ElectionTerminated from '../components/ElectionTerminated';
import { EvotingFeature } from '../interface';
import { ElectionStatus } from '../objects';
import { makeElectionSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_view_single_election>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const ViewSingleElection = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: electionId } = route.params;

  const selectElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectElection);

  if (!election) {
    throw new Error(`Could not find an election with id ${electionId}`);
  }

  switch (election.electionStatus) {
    case ElectionStatus.NOT_STARTED:
      return <ElectionNotStarted election={election} />;
    case ElectionStatus.OPENED:
      return <ElectionOpened election={election} />;
    case ElectionStatus.TERMINATED:
      return <ElectionTerminated election={election} />;
    case ElectionStatus.RESULT:
      return <ElectionResult election={election} />;
    default:
      console.warn('Election Status was undefined in Election display', election);
      return null;
  }
};

export default ViewSingleElection;

export const ViewSingleElectionScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: electionId } = route.params;

  const selectElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectElection);

  if (!election) {
    throw new Error(`Could not find a roll call with id ${electionId}`);
  }

  return <Text style={Typography.topNavigationHeading}>{election.name}</Text>;
};

export const ViewSingleElectionScreenRightHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: electionId, isOrganizer } = route.params;

  const selectionElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectionElection);
  if (!election) {
    throw new Error(`Could not find an election with id ${electionId}`);
  }

  switch (election.electionStatus) {
    case ElectionStatus.NOT_STARTED:
      return <ElectionNotStartedRightHeader election={election} isOrganizer={isOrganizer} />;
    case ElectionStatus.OPENED:
      return <ElectionOpenedRightHeader election={election} isOrganizer={isOrganizer} />;
    case ElectionStatus.TERMINATED:
      return null;
    case ElectionStatus.RESULT:
      return null;
    default:
      throw new Error(`Unkown election status '${election.electionStatus}'`);
  }
};

export const ViewSingleElectionScreen: EvotingFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_view_single_election,
  Component: ViewSingleElection,
  headerTitle: ViewSingleElectionScreenHeader,
  headerRight: ViewSingleElectionScreenRightHeader,
};
