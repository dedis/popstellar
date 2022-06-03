import PropTypes from 'prop-types';
import React from 'react';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { Election } from '../objects';
import { ElectionQuestions } from './ElectionQuestions';

const ElectionResult = ({ election }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <ElectionQuestions election={election} />
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionResult.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionResult;
