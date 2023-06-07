import PropTypes from 'prop-types';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useToast } from 'react-native-toast-notifications';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { Hash, PublicKey } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { requestReopenRollCall } from '../network';
import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';
import RollCallHeader from './RollCallHeader';

const RollCallClosed = ({ rollCall, laoId, isConnected, isOrganizer }: IPropTypes) => {
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

    return [
      {
        title: STRINGS.roll_call_reopen,
        onPress: onReopenRollCall,
        disabled: isConnected !== true,
      } as ToolbarItem,
    ];
  }, [isConnected, isOrganizer, onReopenRollCall]);

  // generate personal token to highlight it in the list
  const generateToken = RollCallHooks.useGenerateToken();
  const [popToken, setPopToken] = useState('');
  generateToken(laoId, rollCall.id)
    .then((token) => {
      setPopToken(token.publicKey.valueOf());
    })
    .catch((err) => console.error(`Could not generate token: ${err}`));

  useEffect(() => {
    // check if the attendee list is sorted alphabetically
    if (rollCall.attendees) {
      const isAttendeeListSorted = rollCall.attendees.reduce<[boolean, PublicKey]>(
        ([isSorted, lastValue], currentValue) => [
          isSorted && lastValue < currentValue,
          currentValue,
        ],
        [true, new PublicKey('')],
      );

      if (!isAttendeeListSorted[0]) {
        toast.show(STRINGS.roll_call_danger_attendee_list_not_sorted, {
          type: 'warning',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      }
    }
  }, [rollCall.attendees, toast]);

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <RollCallHeader rollCall={rollCall} descriptionInitiallyVisible={false} />
      <AttendeeList popTokens={rollCall.attendees || []} personalToken={popToken} />
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  laoId: PropTypes.instanceOf(Hash).isRequired,
  isConnected: PropTypes.bool,
  isOrganizer: PropTypes.bool.isRequired,
};
RollCallClosed.propTypes = propTypes;

RollCallClosed.defaultProps = {
  isConnected: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallClosed;
