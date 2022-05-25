import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo } from 'react';
import { useSelector } from 'react-redux';

import STRINGS from 'resources/strings';

import { Election, ElectionStatus } from '../objects';
import { makeElectionSelector } from '../reducer';
import ElectionNotStarted from './ElectionNotStarted';
import ElectionOpened from './ElectionOpened';
import ElectionResult from './ElectionResult';
import ElectionTerminated from './ElectionTerminated';

/**
 * Component used to display a Election event in the LAO event list
 */

const EventElection = (props: IPropTypes) => {
  const { eventId: electionId, isOrganizer } = props;

  const selectElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectElection);

  if (!election) {
    throw new Error(`Could not find an election with id ${electionId}`);
  }

  const questions = useMemo(
    () => election.questions.map((q) => ({ title: q.question, data: q.ballot_options })),
    [election.questions],
  );

  switch (election.electionStatus) {
    case ElectionStatus.NOT_STARTED:
      return (
        <ElectionNotStarted election={election} questions={questions} isOrganizer={isOrganizer} />
      );
    case ElectionStatus.OPENED:
      return <ElectionOpened election={election} questions={questions} isOrganizer={isOrganizer} />;
    case ElectionStatus.TERMINATED:
      return <ElectionTerminated election={election} />;
    case ElectionStatus.RESULT:
      return <ElectionResult election={election} />;
    default:
      console.warn('Election Status was undefined in Election display', election);
      return null;
  }
};

const propTypes = {
  eventId: PropTypes.string.isRequired,
  isOrganizer: PropTypes.bool,
};
EventElection.propTypes = propTypes;
EventElection.defaultProps = {
  isOrganizer: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventElection;

export const ElectionEventType = {
  eventType: Election.EVENT_TYPE,
  navigationNames: {
    createEvent: STRINGS.organizer_navigation_creation_election,
  },
  Component: EventElection as FunctionComponent<{
    eventId: string;
    isOrganizer: boolean | null | undefined;
  }>,
};
