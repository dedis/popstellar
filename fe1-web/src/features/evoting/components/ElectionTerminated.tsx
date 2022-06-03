import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Election } from '../objects';
import { ElectionQuestions } from './ElectionQuestions';

const ElectionTerminated = ({ election }: IPropTypes) => {
  return (
    <ScreenWrapper>
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
