import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';

import { TimeDisplay } from 'core/components';
import { Typography } from 'core/styles';

import { Election, QuestionResult } from '../objects';
import BarChartDisplay from './BarChartDisplay';

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
  } as TextStyle,
});

const ElectionResult = ({ election }: IPropTypes) => {
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
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionResult.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionResult;
