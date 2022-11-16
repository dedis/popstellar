import PropTypes from 'prop-types';
import React, { useCallback, useMemo } from 'react';
import { useToast } from 'react-native-toast-notifications';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { Hash } from 'core/objects';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { requestOpenRollCall } from '../network';
import { RollCall } from '../objects';
import RollCallHeader from './RollCallHeader';

const RollCallCreated = ({ rollCall, laoId, isOrganizer }: IPropTypes) => {
  const toast = useToast();

  const onOpenRollCall = useCallback(() => {
    requestOpenRollCall(laoId, rollCall.id).catch((e) => {
      toast.show(STRINGS.roll_call_error_open_roll_call, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.debug(STRINGS.roll_call_error_open_roll_call, e);
    });
  }, [toast, rollCall.id, laoId]);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    if (!isOrganizer) {
      return [];
    }

    return [{ title: STRINGS.roll_call_open, onPress: onOpenRollCall } as ToolbarItem];
  }, [isOrganizer, onOpenRollCall]);

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <RollCallHeader rollCall={rollCall} descriptionInitiallyVisible />
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  laoId: PropTypes.instanceOf(Hash).isRequired,
  isOrganizer: PropTypes.bool.isRequired,
};
RollCallCreated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallCreated;
