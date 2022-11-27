import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import { CollapsibleContainer } from 'core/components';
import DateRange from 'core/components/DateRange';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCall } from '../objects';

const RollCallHeader = ({ rollCall, descriptionInitiallyVisible }: IPropTypes) => {
  return (
    <>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{rollCall.name}</Text>
        {'\n'}
        <Text>{rollCall.location}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        <DateRange start={rollCall.proposedStart.toDate()} end={rollCall.proposedEnd.toDate()} />
      </Text>

      {rollCall.description && (
        <CollapsibleContainer
          title={STRINGS.roll_call_description}
          isInitiallyOpen={descriptionInitiallyVisible}>
          <Text style={Typography.paragraph}>{rollCall.description}</Text>
        </CollapsibleContainer>
      )}
    </>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  descriptionInitiallyVisible: PropTypes.bool,
};
RollCallHeader.propTypes = propTypes;

RollCallHeader.defaultProps = {
  descriptionInitiallyVisible: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallHeader;
