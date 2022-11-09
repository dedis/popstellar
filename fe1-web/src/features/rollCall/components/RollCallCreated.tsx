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

import { requestOpenRollCall } from '../network';
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

export const RollCallCreatedRightHeader = ({
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

  const onOpenRollCall = () => {
    requestOpenRollCall(laoId, rollCall.id).catch((e) => {
      toast.show(STRINGS.roll_call_location_error_open_roll_call, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.debug(STRINGS.roll_call_location_error_open_roll_call, e);
    });
  };

  return (
    <PoPTouchableOpacity
      onPress={() =>
        showActionSheet([
          {
            displayName: STRINGS.roll_call_open,
            action: onOpenRollCall,
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
RollCallCreatedRightHeader.propTypes = rightHeaderPropTypes;

type RightHeaderIPropTypes = PropTypes.InferProps<typeof rightHeaderPropTypes>;
