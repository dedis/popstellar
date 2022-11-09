import PropTypes from 'prop-types';
import React from 'react';
import { useToast } from 'react-native-toast-notifications';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { Hash } from 'core/objects';
import { Color, Icon } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { requestReopenRollCall } from '../network';
import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';
import RollCallHeader from './RollCallHeader';

const RollCallClosed = ({ rollCall }: IPropTypes) => {
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

export const RollCallClosedRightHeader = ({
  rollCall,
  laoId,
  isOrganizer,
}: RightHeaderIPropTypes) => {
  const showActionSheet = useActionSheet();
  const toast = useToast();

  // don't show a button for non-organizers
  if (!isOrganizer) {
    return null;
  }

  const makeToastErr = (error: string) => {
    toast.show(error, {
      type: 'danger',
      placement: 'bottom',
      duration: FOUR_SECONDS,
    });
  };

  const onReopenRollCall = () => {
    // Once the roll call is opened the first time, idAlias is defined
    if (rollCall.idAlias) {
      requestReopenRollCall(laoId, rollCall.idAlias).catch((e) => {
        makeToastErr(STRINGS.roll_call_location_error_reopen_roll_call);
        console.debug(STRINGS.roll_call_location_error_reopen_roll_call, e);
      });
    } else {
      makeToastErr(STRINGS.roll_call_location_error_reopen_roll_call_no_alias);
      console.debug(STRINGS.roll_call_location_error_reopen_roll_call_no_alias);
    }
  };

  return (
    <PoPTouchableOpacity
      onPress={() =>
        showActionSheet([
          {
            displayName: STRINGS.roll_call_reopen,
            action: onReopenRollCall,
          },
        ])
      }
      testID="roll_call_options">
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

const rightHeaderPropTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  laoId: PropTypes.instanceOf(Hash).isRequired,
  isOrganizer: PropTypes.bool.isRequired,
};
RollCallClosedRightHeader.propTypes = rightHeaderPropTypes;

type RightHeaderIPropTypes = PropTypes.InferProps<typeof rightHeaderPropTypes>;
