import PropTypes from 'prop-types';
import React, { FunctionComponent, useMemo, useState } from 'react';
import { SectionList, StyleSheet, Text, TextStyle } from 'react-native';
import { Badge } from 'react-native-elements';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { CheckboxList, TimeDisplay, WideButtonView } from 'core/components';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { EvotingHooks } from '../hooks';
import { castVote, openElection, terminateElection } from '../network/ElectionMessageApi';
import {
  Election,
  ElectionStatus,
  ElectionVersion,
  QuestionResult,
  SelectedBallots,
} from '../objects';
import { makeElectionKeySelector } from '../reducer';
import BarChartDisplay from './BarChartDisplay';

/**
 * Component used to display a Election event in the LAO event list
 */

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
  } as TextStyle,
  textOptions: {
    marginHorizontal: Spacing.s,
    fontSize: 16,
    textAlign: 'center',
  } as TextStyle,
  textQuestions: {
    ...Typography.base,
    fontSize: 20,
  } as TextStyle,
});

const EventElection = (props: IPropTypes) => {
  const { event: election, isOrganizer } = props;
  const laoId = EvotingHooks.useCurrentLaoId();

  const electionKeySelector = useMemo(
    () => makeElectionKeySelector(election.id.valueOf()),
    [election.id],
  );
  const electionKey = useSelector(electionKeySelector);

  const toast = useToast();
  const questions = useMemo(
    () => election.questions.map((q) => ({ title: q.question, data: q.ballot_options })),
    [election.questions],
  );
  const [selectedBallots, setSelectedBallots] = useState<SelectedBallots>({});
  const [hasVoted, setHasVoted] = useState(0);

  const onCastVote = () => {
    castVote(laoId, election, selectedBallots)
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

  const onOpenElection = () => {
    console.log('Opening Election');
    openElection(laoId, election)
      .then(() => console.log('Election Opened'))
      .catch((err) => {
        console.error('Could not open election, error:', err);
        toast.show(`Could not open election, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const onTerminateElection = () => {
    console.log('Terminating Election');
    terminateElection(laoId, election)
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

  const canCastVote = election.version !== ElectionVersion.SECRET_BALLOT || electionKey;

  switch (election.electionStatus) {
    case ElectionStatus.NOT_STARTED:
      return (
        <>
          <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
          <SectionList
            sections={questions}
            keyExtractor={(item, index) => item + index}
            renderSectionHeader={({ section: { title } }) => (
              <Text style={styles.textQuestions}>{title}</Text>
            )}
            renderItem={({ item }) => <Text style={styles.textOptions}>{`\u2022 ${item}`}</Text>}
          />
          {isOrganizer && <WideButtonView title="Open election" onPress={onOpenElection} />}
        </>
      );
    case ElectionStatus.OPENED:
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
                    onChange={(values: number[]) =>
                      setSelectedBallots({ ...selectedBallots, [idx]: new Set(values) })
                    }
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
    case ElectionStatus.TERMINATED:
      return (
        <>
          <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
          <Text style={styles.text}>Election Terminated</Text>
          <Text style={styles.text}>Waiting for result</Text>
        </>
      );
    case ElectionStatus.RESULT:
      return (
        <>
          <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
          <Text style={styles.text}>Election Result</Text>
          {election.questionResult &&
            election.questionResult.map((question: QuestionResult) => (
              <BarChartDisplay data={question.result} key={question.id.valueOf()} />
            ))}
        </>
      );
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
