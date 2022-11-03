import PropTypes from 'prop-types';
import React from 'react';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { Election } from '../objects';
import ElectionHeader from './ElectionHeader';
import ElectionQuestions from './ElectionQuestions';

/**
 * Screen component for elections where the result is available
 */
const ElectionResult = ({ election }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <ElectionHeader election={election} />
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
