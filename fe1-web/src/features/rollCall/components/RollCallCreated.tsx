import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import { CollapsibleContainer } from 'core/components';
import DateRange from 'core/components/DateRange';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCall } from '../objects';

const RollCallCreated = ({ rollCall }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{rollCall.name}</Text>
        {'\n'}
        <Text>{rollCall.location}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        <DateRange start={rollCall.proposedStart.toDate()} end={rollCall.proposedStart.toDate()} />
      </Text>

      {rollCall.description && (
        <CollapsibleContainer title={STRINGS.roll_call_description} isInitiallyOpen>
          <Text style={Typography.paragraph}>{rollCall.description}</Text>
        </CollapsibleContainer>
      )}
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
};
RollCallCreated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallCreated;
