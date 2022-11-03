import PropTypes from 'prop-types';
import React from 'react';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { RollCall } from '../objects';
import RollCallHeader from './RollCallHeader';

const RollCallCreated = ({ rollCall }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <RollCallHeader rollCall={rollCall} descriptionInitiallyVisible />
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
};
RollCallCreated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallCreated;
