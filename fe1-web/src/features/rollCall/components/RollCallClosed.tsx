import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';
import ReactTimeago from 'react-timeago';

import { CollapsibleContainer } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';

const RollCallClosed = ({ rollCall }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{rollCall.name}</Text>
        {'\n'}
        <Text>{rollCall.location}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        {STRINGS.general_ended} <ReactTimeago date={rollCall.end.toDate()} />
      </Text>

      {rollCall.description && (
        <CollapsibleContainer title={STRINGS.roll_call_description} isInitiallyOpen={false}>
          <Text style={Typography.paragraph}>{rollCall.description}</Text>
        </CollapsibleContainer>
      )}

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
