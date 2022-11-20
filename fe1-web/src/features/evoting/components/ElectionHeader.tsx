import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import DateRange from 'core/components/DateRange';
import { Typography } from 'core/styles';

import { Election } from '../objects';

const ElectionHeader = ({ election }: IPropTypes) => {
  return (
    <Text style={Typography.paragraph}>
      <Text style={[Typography.base, Typography.important]}>{election.name}</Text>
      {'\n'}
      <Text style={Typography.paragraph}>
        <DateRange start={election.start.toDate()} end={election.end.toDate()} />
      </Text>
    </Text>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionHeader.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionHeader;
