import PropTypes from 'prop-types';
import React, { useMemo, useState } from 'react';
import { Text } from 'react-native';
import { Badge } from 'react-native-elements';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { CheckboxList, TimeDisplay, WideButtonView } from 'core/components';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { castVote, terminateElection } from '../network/ElectionMessageApi';
import { Election, ElectionVersion, SelectedBallots } from '../objects';
import { makeElectionKeySelector } from '../reducer';

const ElectionOpened = ({ election, questions, isOrganizer }: IPropTypes) => {
  const toast = useToast();

  const [selectedBallots, setSelectedBallots] = useState<SelectedBallots>({});
  const [hasVoted, setHasVoted] = useState(0);

  const electionKeySelector = useMemo(
    () => makeElectionKeySelector(election.id.valueOf()),
    [election.id],
  );
  const electionKey = useSelector(electionKeySelector);

  const canCastVote = !!(election.version !== ElectionVersion.SECRET_BALLOT || electionKey);

  const onCastVote = () => {
    castVote(election, electionKey || undefined, selectedBallots)
      .then(() => setHasVoted((prev) => prev + 1))
      .catch((err) => {
        console.error('Could not cast Vote, error:', err);
        toast.show(`Could not cast Vote, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const onTerminateElection = () => {
    console.log('Terminating Election');
    terminateElection(election)
      .then(() => console.log('Election Terminated'))
      .catch((err) => {
        console.error('Could not terminate election, error:', err);
        toast.show(`Could not terminate election, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  return (
    <>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
      {
        // in case the election is a secret ballot election, tell the
        // user if no election key has been received yet
        canCastVote ? (
          <>
            {questions.map((q, idx) => (
              <CheckboxList
                key={q.title + idx.toString()}
                title={q.title}
                values={q.data}
                clickableOptions={1}
                onChange={(values: number[]) => {
                  if (values.length > 1) {
                    throw new Error('Only single vote elections are supported');
                  }

                  setSelectedBallots({ ...selectedBallots, [idx]: values[0] });
                }}
              />
            ))}
            <WideButtonView title={STRINGS.cast_vote} onPress={onCastVote} />
            <Badge value={hasVoted} status="success" />
            {isOrganizer && (
              <WideButtonView
                title="Terminate Election / Tally Votes"
                onPress={onTerminateElection}
              />
            )}
          </>
        ) : (
          <Text>{STRINGS.election_wait_for_election_key}</Text>
        )
      }
    </>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  questions: PropTypes.arrayOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      data: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
  ).isRequired,
  isOrganizer: PropTypes.bool,
};
ElectionOpened.propTypes = propTypes;

ElectionOpened.defaultProps = {
  isOrganizer: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionOpened;
