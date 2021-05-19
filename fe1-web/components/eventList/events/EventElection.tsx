import React, { useState } from 'react';
import ParagraphBlock from 'components/ParagraphBlock';
import PropTypes from 'prop-types';
import { Election, EventTags, Hash, Timestamp, Vote } from 'model/objects';
import {
  SectionList, StyleSheet, Text, TextStyle,
} from 'react-native';
import { Typography } from 'styles';
import { castVote } from 'network';
import CheckboxList from 'components/CheckboxList';
import WideButtonView from 'components/WideButtonView';

/**
 * Component used to display a Election event in the LAO event list
 */

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
  } as TextStyle,
});

const EventElection = (props: IPropTypes) => {
  const { event } = props;
  const startsAtString = event.start.before(Timestamp.EpochNow()) ? 'Started at' : 'Starts at';
  const endsAtString = event.end.before(Timestamp.EpochNow()) ? 'Ended at' : 'Ends at';
  const questions = event.questions.map((q) => ({ title: q.question, data: q.ballot_options }));
  const [selectedBallots, setSelectedBallots] = useState(new Array(questions.length).fill([]));
  const [hasVoted, setHasVoted] = useState(false);
  const isRunning = (event.start.before(Timestamp.EpochNow())
    && event.end.after(Timestamp.EpochNow()));
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
    // Todo: Get index of selected ballot options, convert to string and put in list
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
    console.log('Casting vote: ', selectedBallots);
    castVote(event.id, refactorVotes(selectedBallots))
      .then(() => setHasVoted(true))
      .catch((err) => {
        console.error('Could not cast Vote, error:', err);
      });
  };

  let electionScreen;
  if (hasVoted) {
    electionScreen = <Text style={styles.text}>Vote Confirmed</Text>;
  } else if (isRunning) {
    electionScreen = [(questions.map((q, idx) => (
      <CheckboxList
        title={q.title}
        values={q.data}
        onChange={(values: number[]) => updateSelectedBallots(values, idx)}
      />
    ))),
      <WideButtonView
        title="Cast Vote"
        onPress={onCastVote}
      />];
  } else {
    electionScreen = (
      <SectionList
        sections={questions}
        keyExtractor={(item, index) => item + index}
        renderSectionHeader={({ section: { title } }) => (
          <Text>{title}</Text>
        )}
        renderItem={({ item }) => (
          <Text>{`\u2022 ${item}`}</Text>
        )}
      />
    );
  }

  return (
    <>
      <ParagraphBlock text={`${startsAtString} ${event.start.timestampToString()}`} />
      <ParagraphBlock text={`${endsAtString} ${event.end.timestampToString()}`} />
      {electionScreen}
    </>
  );
};

const propTypes = {
  event: PropTypes.instanceOf(Election).isRequired,
};
EventElection.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventElection;
