import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { Color, Icon, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { openElection } from '../network/ElectionMessageApi';
import { Election } from '../objects';
import ElectionQuestions from './ElectionQuestions';

/**
 * Screen component for not started elections
 */
const ElectionNotStarted = ({ election }: IPropTypes) => {
  return (
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.general_starting_at}</Text>
        {'\n'}
        <Text>
          {election.start.toDate().toLocaleDateString()}{' '}
          {election.start.toDate().toLocaleTimeString()}
        </Text>
      </Text>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.general_ending_at}</Text>
        {'\n'}
        <Text>
          {election.end.toDate().toLocaleDateString()} {election.end.toDate().toLocaleTimeString()}
        </Text>
      </Text>
      <ElectionQuestions election={election} />
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionNotStarted.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionNotStarted;

/**
 * Component that is rendered in the top right of the navigation bar for not started elections.
 * Allows for example to show icons that then trigger different actions
 */
export const ElectionNotStartedRightHeader = (props: RightHeaderIPropTypes) => {
  const { election, isOrganizer } = props;

  const showActionSheet = useActionSheet();
  const toast = useToast();

  const onOpenElection = () => {
    console.log('Opening Election');
    openElection(election)
      .then(() => console.log('Election Opened'))
      .catch((err) => {
        console.error('Could not open election, error:', err);
        toast.show(`Could not open election, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  // don't show a button for non-organizers
  if (!isOrganizer) {
    return null;
  }

  return (
    <PoPTouchableOpacity
      testID="election_option_selector"
      onPress={() =>
        showActionSheet([{ displayName: STRINGS.election_open, action: onOpenElection }])
      }>
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

const rightHeaderPropTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  isOrganizer: PropTypes.bool,
};

type RightHeaderIPropTypes = PropTypes.InferProps<typeof rightHeaderPropTypes>;

ElectionNotStartedRightHeader.propTypes = rightHeaderPropTypes;

ElectionNotStartedRightHeader.defaultProps = {
  isOrganizer: false,
};
