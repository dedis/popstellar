import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Hash, PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import RollCallClosed from '../components/RollCallClosed';
import RollCallCreated from '../components/RollCallCreated';
import RollCallOpen from '../components/RollCallOpen';
import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
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
  const {
    eventId: rollCallId,
    isOrganizer,
    attendeePopTokens: attendeePopTokensStrings,
  } = route.params;

  const selectRollCall = useMemo(() => makeRollCallSelector(new Hash(rollCallId)), [rollCallId]);
  const laoId = RollCallHooks.useCurrentLaoId();
  const isConnected = RollCallHooks.useConnectedToLao();
  const rollCall = useSelector(selectRollCall);

  const attendeePopTokens = useMemo(() => {
    // if attendeePopTokens is defined, it means we come from the scanner -> take the tokens from there
    // otherwise, take the tokens from the roll call
    if (attendeePopTokensStrings) {
      return attendeePopTokensStrings.map((k) => new PublicKey(k));
    }
    return rollCall?.attendees || [];
  }, [attendeePopTokensStrings, rollCall?.attendees]);

  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  switch (rollCall.status) {
    case RollCallStatus.CREATED:
      return (
        <RollCallCreated
          rollCall={rollCall}
          laoId={laoId}
          isConnected={isConnected}
          isOrganizer={isOrganizer}
        />
      );
    case RollCallStatus.REOPENED:
    case RollCallStatus.OPENED:
      return (
        <RollCallOpen
          rollCall={rollCall}
          laoId={laoId}
          isConnected={isConnected}
          isOrganizer={isOrganizer}
          scannedPopTokens={attendeePopTokens}
        />
      );
    case RollCallStatus.CLOSED:
      return (
        <RollCallClosed
          rollCall={rollCall}
          laoId={laoId}
          isConnected={isConnected}
          isOrganizer={isOrganizer}
        />
      );
    default:
      console.warn(`Unkown roll call status '${rollCall.status}' in ViewSingleRollCall`);
      return null;
  }
};

export default ViewSingleRollCall;

export const ViewSingleRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.events_view_single_roll_call,
  Component: ViewSingleRollCall,
  headerTitle: STRINGS.roll_call_event_name,
};
