import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import { TimeDisplay } from 'core/components';
import { Typography } from 'core/styles';

import { Election } from '../objects';

const ElectionTerminated = ({ election }: IPropTypes) => {
  return (
    <>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
      <Text style={Typography.base}>Election Terminated</Text>
      <Text style={Typography.base}>Waiting for result</Text>
    </>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionTerminated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionTerminated;
