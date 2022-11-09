import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import STRINGS from 'resources/strings';

import RollCallClosed, { RollCallClosedRightHeader } from '../components/RollCallClosed';
import RollCallCreated, { RollCallCreatedRightHeader } from '../components/RollCallCreated';
import RollCallOpen, { RollCallOpenRightHeader } from '../components/RollCallOpen';
import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
import { RollCallStatus } from '../objects';
import { makeRollCallSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_view_single_roll_call>,
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
  const route = useRoute<NavigationProps['route']>();
  const { eventId: rollCallId, isOrganizer, attendeePopTokens } = route.params;

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);
  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  const laoId = RollCallHooks.useAssertCurrentLaoId();

  switch (rollCall.status) {
    case RollCallStatus.CREATED:
      return (
        <RollCallCreatedRightHeader rollCall={rollCall} laoId={laoId} isOrganizer={isOrganizer} />
      );
    case RollCallStatus.REOPENED:
    case RollCallStatus.OPENED:
      return (
        <RollCallOpenRightHeader
          rollCall={rollCall}
          laoId={laoId}
          isOrganizer={isOrganizer}
          attendeePopTokens={attendeePopTokens}
        />
      );
    case RollCallStatus.CLOSED:
      return (
        <RollCallClosedRightHeader rollCall={rollCall} laoId={laoId} isOrganizer={isOrganizer} />
      );
    default:
      console.warn('Roll Call Status was undefined in EventRollCall');
      return null;
  }
};

export const ViewSingleRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_view_single_roll_call,
  Component: ViewSingleRollCall,
  headerTitle: STRINGS.roll_call_event_name,
  headerRight: ViewSinglRollCallScreenRightHeader,
};
