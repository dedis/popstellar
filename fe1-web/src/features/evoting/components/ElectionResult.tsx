import PropTypes from 'prop-types';
import React from 'react';
import { Text, View } from 'react-native';

import { TimeDisplay } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';

import { Election, QuestionResult } from '../objects';
import BarChartDisplay from './BarChartDisplay';

const ElectionResult = ({ election }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
      <Text style={Typography.base}>Election Results</Text>
      {election.questionResult &&
        election.questionResult.map((questionResult: QuestionResult) => {
          const question = election.questions.find((q) => q.id === questionResult.id);

          return question ? (
            <View>
              <Text style={Typography.base}>{question.question}</Text>
              <BarChartDisplay data={questionResult.result} key={questionResult.id.valueOf()} />
            </View>
          ) : null;
        })}
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionResult.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionResult;
