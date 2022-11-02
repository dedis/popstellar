import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import { ActionSheetOption, useActionSheet } from 'core/hooks/ActionSheet';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { PublicKey } from 'core/objects';
import { Color, Icon } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import RollCallClosed from '../components/RollCallClosed';
import RollCallCreated from '../components/RollCallCreated';
import RollCallOpen from '../components/RollCallOpen';
import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
import { requestCloseRollCall, requestOpenRollCall, requestReopenRollCall } from '../network';
import { RollCallStatus } from '../objects';
import { makeRollCallSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.events_view_single_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * Screen for viewing a single roll call
 */
const ViewSingleRollCall = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: rollCallId, isOrganizer, attendeePopTokens } = route.params;

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);

  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  switch (rollCall.status) {
    case RollCallStatus.CREATED:
      return <RollCallCreated rollCall={rollCall} />;
    case RollCallStatus.REOPENED:
    case RollCallStatus.OPENED:
      return (
        <RollCallOpen
          rollCall={rollCall}
          isOrganizer={isOrganizer}
          scannedPopTokens={attendeePopTokens}
        />
      );
    case RollCallStatus.CLOSED:
      return <RollCallClosed rollCall={rollCall} />;
    default:
      console.warn('Roll Call Status was undefined in EventRollCall');
      return null;
  }
};

export default ViewSingleRollCall;

/**
 * Component rendered in the top right of the navigation bar when looking at a roll call.
 * Allows the user to trigger different actions by pressing on the options button.
 */
export const ViewSinglRollCallScreenRightHeader = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();
  const { eventId: rollCallId, isOrganizer, attendeePopTokens } = route.params;

  const toast = useToast();

  const showActionSheet = useActionSheet();

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);
  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  const laoId = RollCallHooks.useAssertCurrentLaoId();
  if (!laoId) {
    throw new Error('no LAO is currently active');
  }

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

  const onOpenRollCall = () => {
    requestOpenRollCall(laoId, rollCall.id).catch((e) => {
      makeToastErr(STRINGS.roll_call_location_error_open_roll_call);
      console.debug(STRINGS.roll_call_location_error_open_roll_call, e);
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

  const onCloseRollCall = async () => {
    // get the public key as strings from the existing rollcall
    const previousAttendees = (rollCall.attendees || []).map((key) => key.valueOf());
    // add the create a set of all attendees (takes care of deduplication)
    const allAttendees = new Set([...previousAttendees, ...(attendeePopTokens || [])]);
    // create PublicKey instances from the set of strings
    const attendeesList = [...allAttendees].map((key: string) => new PublicKey(key));

    if (!rollCall.idAlias) {
      throw new Error('Trying to close a roll call that has no idAlias defined');
    }

    try {
      await requestCloseRollCall(laoId, rollCall.idAlias, attendeesList);
      navigation.navigate(STRINGS.navigation_lao_events_home);
    } catch (err) {
      console.log(err);
      toast.show(STRINGS.roll_call_location_error_close_roll_call, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  const getActionOptions = (status: RollCallStatus): ActionSheetOption[] => {
    switch (status) {
      case RollCallStatus.CREATED:
        return [
          {
            displayName: STRINGS.roll_call_open,
            action: onOpenRollCall,
          },
        ];
      case RollCallStatus.OPENED:
      case RollCallStatus.REOPENED:
        return [
          {
            displayName: STRINGS.roll_call_close,
            action: onCloseRollCall,
          },
        ];
      case RollCallStatus.CLOSED:
        return [
          {
            displayName: STRINGS.roll_call_reopen,
            action: onReopenRollCall,
          },
        ];

      default:
        throw new Error(`Unknown roll call status '${status}'`);
    }
  };

  return (
    <PoPTouchableOpacity
      onPress={() => showActionSheet(getActionOptions(rollCall.status))}
      testID="roll_call_options">
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

export const ViewSingleRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.events_view_single_roll_call,
  Component: ViewSingleRollCall,
  headerTitle: STRINGS.roll_call_event_name,
  headerRight: ViewSinglRollCallScreenRightHeader,
};
