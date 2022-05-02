import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo } from 'react';
import { useSelector } from 'react-redux';

import { Election, ElectionStatus, ElectionVersion } from '../objects';
import { makeElectionKeySelector } from '../reducer';
import ElectionNotStarted from './ElectionNotStarted';
import ElectionOpened from './ElectionOpened';
import ElectionResult from './ElectionResult';
import ElectionTerminated from './ElectionTerminated';

/**
 * Component used to display a Election event in the LAO event list
 */

const EventElection = (props: IPropTypes) => {
  const { event: election, isOrganizer } = props;

  const electionKeySelector = useMemo(
    () => makeElectionKeySelector(election.id.valueOf()),
    [election.id],
  );
  const electionKey = useSelector(electionKeySelector);

  const questions = useMemo(
    () => election.questions.map((q) => ({ title: q.question, data: q.ballot_options })),
    [election.questions],
  );

  const canCastVote = !!(election.version !== ElectionVersion.SECRET_BALLOT || electionKey);

  switch (election.electionStatus) {
    case ElectionStatus.NOT_STARTED:
      return (
        <ElectionNotStarted election={election} questions={questions} isOrganizer={isOrganizer} />
      );
    case ElectionStatus.OPENED:
      return (
        <ElectionOpened
          election={election}
          questions={questions}
          isOrganizer={isOrganizer}
          canCastVote={canCastVote}
        />
      );
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
  event: PropTypes.instanceOf(Election).isRequired,
  isOrganizer: PropTypes.bool,
};
EventElection.propTypes = propTypes;
EventElection.defaultProps = {
  isOrganizer: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventElection;

export const ElectionEventTypeComponent = {
  isOfType: (event: unknown) => event instanceof Election,
  Component: EventElection as FunctionComponent<{
    event: unknown;
    isOrganizer: boolean | null | undefined;
  }>,
};
