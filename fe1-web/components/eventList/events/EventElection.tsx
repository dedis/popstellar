import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import {
  Election, ElectionStatus, EventTags, Hash, Timestamp, Vote,
} from 'model/objects';
import {
  SectionList, StyleSheet, Text, TextStyle,
} from 'react-native';
import { Typography } from 'styles';
import { castVote } from 'network';
import CheckboxList from 'components/CheckboxList';
import WideButtonView from 'components/WideButtonView';
import TimeDisplay from 'components/TimeDisplay';
import STRINGS from 'res/strings';
import { Badge, LinearProgress } from 'react-native-elements';
import * as Spacing from '../../../styles/spacing';

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
  const { event } = props;
  const { isOrganizer } = props;
  const questions = event.questions.map((q) => ({ title: q.question, data: q.ballot_options }));
  const [selectedBallots, setSelectedBallots] = useState(new Array(questions.length).fill([]));
  const [hasVoted, setHasVoted] = useState(0);
  const [status, setStatus] = useState(event.getStatus());
  const untilStart = (event.start.valueOf() - Timestamp.EpochNow().valueOf()) * 1000;
  const untilEnd = (event.end.valueOf() - Timestamp.EpochNow().valueOf()) * 1000;

  const updateSelectedBallots = (values: number[], idx: number) => {
    setSelectedBallots((prev) => prev.map((item, id) => ((idx === id) ? values : item)));
  };
  const concatenateIndexes = (indexes: number[]) => {
    let concatenated = '';
    indexes.forEach((index) => {
      concatenated += index.toString();
    });
    return concatenated;
  };

  const refactorVotes = (selected: number[][]) => {
    // id: SHA256('Vote'||election_id||question_id||(vote_index(es)|write_in))
    // concatenate vote indexes - must use delimiter"
    const votes: Vote[] = selected.map((item, idx) => ({
      id: Hash.fromStringArray(
        EventTags.VOTE, event.id.toString(), event.questions[idx].id, concatenateIndexes(item),
      ),
      question: new Hash(event.questions[idx].id),
      vote: item,
    }));
    return votes;
  };

  const onCastVote = () => {
    castVote(event.id, refactorVotes(selectedBallots))
      .then(() => setHasVoted((prev) => prev + 1))
      .catch((err) => {
        console.error('Could not cast Vote, error:', err);
      });
  };

  const onTerminateElection = () => {
    console.log('Terminating Election');
  };

  // This makes sure the screen gets updated when the event starts
  useEffect(() => {
    if (untilStart >= 0) {
      const startTimer = setTimeout(() => {
        setStatus(ElectionStatus.RUNNING);
      }, untilStart);
      return () => clearTimeout(startTimer);
    }
    return () => {};
  }, []);

  // This makes sure the screen gets updated when the event ends - user can't vote anymore
  useEffect(() => {
    if (untilEnd >= 0) {
      const endTimer = setTimeout(() => {
        setStatus(ElectionStatus.FINISHED);
      }, untilEnd);
      return () => clearTimeout(endTimer);
    }
    return () => {};
  }, []);

  let electionScreen;
  if (status === ElectionStatus.FINISHED) {
    electionScreen = (
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
  } else if (status === ElectionStatus.RUNNING) {
    electionScreen = (
      <>
        {(questions.map((q, idx) => (
          <CheckboxList
            title={q.title}
            values={q.data}
            onChange={(values: number[]) => updateSelectedBallots(values, idx)}
          />
        )))}
        <WideButtonView
          title={STRINGS.cast_vote}
          onPress={onCastVote}
        />
        <Badge value={hasVoted} status="success" />
      </>
    );
  } else {
    electionScreen = (
      <SectionList
        sections={questions}
        keyExtractor={(item, index) => item + index}
        renderSectionHeader={({ section: { title } }) => (
          <Text style={styles.textQuestions}>{title}</Text>
        )}
        renderItem={({ item }) => (
          <Text style={styles.textOptions}>{`\u2022 ${item}`}</Text>
        )}
      />
    );
  }

  return (
    <>
      <TimeDisplay start={event.start.valueOf()} end={event.end.valueOf()} />
      {electionScreen}
    </>
  );
};

const propTypes = {
  event: PropTypes.instanceOf(Election).isRequired,
  isOrganizer: PropTypes.bool.isRequired,
};
EventElection.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventElection;
