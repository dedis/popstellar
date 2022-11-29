import PropTypes from 'prop-types';
import React, { useCallback, useMemo } from 'react';
import { useToast } from 'react-native-toast-notifications';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { openElection } from '../network/ElectionMessageApi';
import { Election } from '../objects';
import ElectionHeader from './ElectionHeader';
import ElectionQuestions from './ElectionQuestions';

/**
 * Screen component for not started elections
 */
const ElectionNotStarted = ({ election, isConnected, isOrganizer }: IPropTypes) => {
  const toast = useToast();

  const onOpenElection = useCallback(() => {
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
  }, [toast, election]);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    if (!isOrganizer) {
      return [];
    }

    return [
      {
        id: 'election_option_selector',
        title: STRINGS.election_open,
        onPress: onOpenElection,
        disabled: isConnected !== true,
      },
    ] as ToolbarItem[];
  }, [isConnected, isOrganizer, onOpenElection]);

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <ElectionHeader election={election} />
      <ElectionQuestions election={election} />
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  isConnected: PropTypes.bool,
  isOrganizer: PropTypes.bool.isRequired,
};
ElectionNotStarted.propTypes = propTypes;

ElectionNotStarted.defaultProps = {
  isConnected: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionNotStarted;
