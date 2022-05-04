import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';

import { TimeDisplay } from 'core/components';
import { Typography } from 'core/styles';

import { Election } from '../objects';

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
  } as TextStyle,
});

const ElectionTerminated = ({ election }: IPropTypes) => {
  return (
    <>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
      <Text style={styles.text}>Election Terminated</Text>
      <Text style={styles.text}>Waiting for result</Text>
    </>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionTerminated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionTerminated;
