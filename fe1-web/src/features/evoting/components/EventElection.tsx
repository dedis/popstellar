import React, { FunctionComponent, useMemo, useState } from 'react';
import { SectionList, StyleSheet, Text, TextStyle } from 'react-native';
import { Badge } from 'react-native-elements';
import PropTypes from 'prop-types';
import { useToast } from 'react-native-toast-notifications';

import { Spacing, Typography } from 'core/styles';
import { CheckboxList, TimeDisplay, WideButtonView } from 'core/components';
import STRINGS from 'resources/strings';
import { FOUR_SECONDS } from 'resources/const';

import { castVote, terminateElection } from '../network/ElectionMessageApi';
import { Election, ElectionStatus, QuestionResult } from '../objects';
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

const EventElection: FunctionComponent<IPropTypes> = (props) => {
  const { election } = props;
  const { isOrganizer } = props;
  const toast = useToast();
  const questions = useMemo(
    () => election.questions.map((q) => ({ title: q.question, data: q.ballot_options })),
    [election.questions],
  );
  const [selectedBallots, setSelectedBallots] = useState<{ [questionIndex: number]: Set<number> }>(
    {},
  );
  const [hasVoted, setHasVoted] = useState(0);

  const onCastVote = () => {
    castVote(election, selectedBallots)
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

  // Here we use the election object form the redux store in order to see the electionStatus
  // update when an  incoming electionEnd or electionResult message comes
  // (in handler/ElectionHandler.ts)
  const getElectionDisplay = (status: ElectionStatus) => {
    switch (status) {
      case ElectionStatus.NOT_STARTED:
        return (
          <SectionList
            sections={questions}
            keyExtractor={(item, index) => item + index}
            renderSectionHeader={({ section: { title } }) => (
              <Text style={styles.textQuestions}>{title}</Text>
            )}
            renderItem={({ item }) => <Text style={styles.textOptions}>{`\u2022 ${item}`}</Text>}
          />
        );
      case ElectionStatus.RUNNING:
        return (
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
        );
      case ElectionStatus.FINISHED:
        return (
          <>
            <Text style={styles.text}>Election finished</Text>
            {isOrganizer && (
              <WideButtonView
                title="Terminate Election / Tally Votes"
                onPress={onTerminateElection}
              />
            )}
          </>
        );
      case ElectionStatus.TERMINATED:
        return (
          <>
            <Text style={styles.text}>Election Terminated</Text>
            <Text style={styles.text}>Waiting for result</Text>
          </>
        );
      case ElectionStatus.RESULT:
        return (
          <>
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

  return (
    <>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
      {getElectionDisplay(election.electionStatus)}
    </>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  isOrganizer: PropTypes.bool.isRequired,
};
EventElection.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventElection;
