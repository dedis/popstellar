import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import DateRange from 'core/components/DateRange';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Election } from '../objects';
import ElectionQuestions from './ElectionQuestions';

/**
 * Screen component for terminated elections
 */
const ElectionTerminated = ({ election }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{election.name}</Text>
        {'\n'}
        <Text style={Typography.paragraph}>
          <DateRange start={election.start.toDate()} end={election.end.toDate()} />
        </Text>
      </Text>

      <Text style={Typography.base}>{STRINGS.election_terminated_description}</Text>

      <ElectionQuestions election={election} />
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionTerminated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionTerminated;
