import PropTypes from 'prop-types';
import React, { useCallback, useMemo } from 'react';
import { useToast } from 'react-native-toast-notifications';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { Hash } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { requestReopenRollCall } from '../network';
import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';
import RollCallHeader from './RollCallHeader';

const RollCallClosed = ({ rollCall, laoId, isOrganizer }: IPropTypes) => {
  const toast = useToast();

  const makeToastErr = useCallback(
    (error: string) => {
      toast.show(error, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    },
    [toast],
  );

  const onReopenRollCall = useCallback(() => {
    // Once the roll call is opened the first time, idAlias is defined
    if (!rollCall.idAlias) {
      makeToastErr(STRINGS.roll_call_error_reopen_roll_call_no_alias);
      console.warn(STRINGS.roll_call_error_reopen_roll_call_no_alias);
      return;
    }

    requestReopenRollCall(laoId, rollCall.idAlias).catch((e) => {
      makeToastErr(STRINGS.roll_call_error_reopen_roll_call);
      console.debug(STRINGS.roll_call_error_reopen_roll_call, e);
    });
  }, [makeToastErr, rollCall, laoId]);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    if (!isOrganizer) {
      return [];
    }

    return [{ title: STRINGS.roll_call_reopen, onPress: onReopenRollCall } as ToolbarItem];
  }, [isOrganizer, onReopenRollCall]);

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <RollCallHeader rollCall={rollCall} descriptionInitiallyVisible={false} />
      <AttendeeList popTokens={rollCall.attendees || []} />
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  laoId: PropTypes.instanceOf(Hash).isRequired,
  isOrganizer: PropTypes.bool.isRequired,
};
RollCallClosed.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallClosed;
