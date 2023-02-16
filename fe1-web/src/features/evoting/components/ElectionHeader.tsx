import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import DateRange from 'core/components/DateRange';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Election, ElectionStatus } from '../objects';

const LABEL_BY_ELECTION_STATUS: Record<ElectionStatus, string> = {
  [ElectionStatus.NOT_STARTED]: STRINGS.election_status_not_started,
  [ElectionStatus.OPENED]: STRINGS.election_status_opened,
  [ElectionStatus.TERMINATED]: STRINGS.election_status_terminated,
  [ElectionStatus.RESULT]: STRINGS.election_status_results,
};

const ElectionHeader = ({ election }: IPropTypes) => {
  return (
    <Text style={Typography.paragraph}>
      <Text style={[Typography.base, Typography.important]}>{election.name}</Text>
      {'\n'}
      <Text style={Typography.paragraph}>
        <DateRange start={election.start.toDate()} end={election.end.toDate()} />
      </Text>
      {'\n'}
      <Text style={Typography.paragraph}>{LABEL_BY_ELECTION_STATUS[election.electionStatus]}</Text>
    </Text>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionHeader.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionHeader;
