import PropTypes from 'prop-types';
import React from 'react';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';
import RollCallHeader from './RollCallHeader';

const RollCallClosed = ({ rollCall }: IPropTypes) => {
  if (!rollCall.end) {
    throw new Error('rollCall.end should always be defined for closed roll calls');
  }

  return (
    <ScreenWrapper>
      <RollCallHeader rollCall={rollCall} descriptionInitiallyVisible={false} />
      <AttendeeList popTokens={rollCall.attendees || []} />
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
};
RollCallClosed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallClosed;
