import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React, { useMemo, useState } from 'react';
import { useSelector } from 'react-redux';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Hash, PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { ConfirmModal } from '../../../core/components';
import backButton from '../../../core/components/BackButton';
import ButtonPadding from '../../../core/components/ButtonPadding';
import PoPIcon from '../../../core/components/PoPIcon';
import PoPTouchableOpacity from '../../../core/components/PoPTouchableOpacity';
import { Color, Icon } from '../../../core/styles';
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

/**
 * Return button that shows a confirmation modal if there are new scanned attendees
 * to prevent the user from losing the new scanned attendees
 */
const ReturnButton = ({ padding }: IPropTypes) => {
  const navigationRoute = useRoute<NavigationProps['route']>();
  const navigation = useNavigation<NavigationProps>();
  const { attendeePopTokens: attendeePopTokensStrings, eventId: rollCallId } =
    navigationRoute.params;

  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const selectRollCall = useMemo(() => makeRollCallSelector(new Hash(rollCallId)), [rollCallId]);
  const rollCall = useSelector(selectRollCall);

  if (
    rollCall === undefined ||
    rollCall.attendees === undefined ||
    attendeePopTokensStrings === undefined
  ) {
    return backButton({ padding });
  }

  // no new scanned attendees
  if (attendeePopTokensStrings?.length <= rollCall.attendees.length) {
    return backButton({ padding });
  }

  // new scanned attendees -> leaving will not save the new attendees
  return (
    <>
      <PoPTouchableOpacity onPress={() => setShowConfirmModal(true)}>
        <PoPIcon name="arrowBack" color={Color.inactive} size={Icon.size} />
      </PoPTouchableOpacity>
      <ButtonPadding paddingAmount={padding || 0} nextToIcon />
      <ConfirmModal
        onConfirmPress={navigation.goBack}
        visibility={showConfirmModal}
        description="Description"
        title="Title"
        setVisibility={setShowConfirmModal}
      />
    </>
  );
};

const propTypes = {
  padding: PropTypes.number,
};

ReturnButton.propTypes = propTypes;

ReturnButton.defaultProps = {
  padding: 0,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ViewSingleRollCall;

export const ViewSingleRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.events_view_single_roll_call,
  Component: ViewSingleRollCall,
  headerTitle: STRINGS.roll_call_event_name,
  headerLeft: ReturnButton,
};
