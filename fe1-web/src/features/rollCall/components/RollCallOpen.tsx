import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import ReactTimeago from 'react-timeago';

import { CollapsibleContainer, QRCode } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import { ScannablePopToken } from 'core/objects/ScannablePopToken';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { requestCloseRollCall } from '../network';
import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_view_single_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const RollCallOpen = ({ rollCall, laoId, isOrganizer, scannedPopTokens }: IPropTypes) => {
  const generateToken = RollCallHooks.useGenerateToken();
  const hasSeed = RollCallHooks.useHasSeed();
  const toast = useToast();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const [popToken, setPopToken] = useState('');
  const [hasWalletBeenInitialized, setHasWalletBeenInitialized] = useState(hasSeed());

  const onAddAttendees = useCallback(() => {
    // Once the roll call is opened the first time, idAlias is defined
    if (!rollCall.idAlias) {
      toast.show(STRINGS.roll_call_location_error_scanning_no_alias, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.warn(STRINGS.roll_call_location_error_scanning_no_alias);
      return;
    }
    
    navigation.navigate(STRINGS.navigation_app_lao, {
      screen: STRINGS.navigation_lao_events,
      params: {
        screen: STRINGS.navigation_lao_events_open_roll_call,
        params: {
          rollCallId: rollCall.id.toString(),
          attendeePopTokens: (scannedPopTokens || []).map((e) => e.valueOf()),
        },
      },
    });
  }, [toast, navigation, rollCall, scannedPopTokens]);

  const onCloseRollCall = useCallback(async () => {
    // get the public key as strings from the existing rollcall
    const previousAttendees = (rollCall.attendees || []).map((key) => key.valueOf());
    // add the create a set of all attendees (takes care of deduplication)
    const allAttendees = new Set([...previousAttendees, ...(scannedPopTokens || [])]);
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
  }, [toast, navigation, rollCall, laoId, scannedPopTokens]);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    if (!isOrganizer) {
      return [];
    }

    return [
      { title: STRINGS.roll_call_close, onPress: onCloseRollCall, buttonStyle: 'secondary' },
      { title: STRINGS.roll_call_scan_attendees, onPress: onAddAttendees },
    ] as ToolbarItem[];
  }, [isOrganizer, onCloseRollCall, onAddAttendees]);

  // re-check if wallet has been initialized after focus events
  useEffect(() => {
   let unsubscribe = navigation.addListener('focus', () => {
      setHasWalletBeenInitialized(hasSeed());
    });
    
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return unsubscribe;
  }, [navigation, hasSeed]);

  useEffect(() => {
    if (!hasWalletBeenInitialized || !laoId || !rollCall?.id) {
      return;
    }

    // Here we get the pop-token to display in the QR code
    generateToken(laoId, rollCall.id)
      .then((token) => setPopToken(token.publicKey.valueOf()))
      .catch((err) => console.error(`Could not generate token: ${err}`));
  }, [hasWalletBeenInitialized, generateToken, laoId, rollCall]);

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{rollCall.name}</Text>
        {'\n'}
        <Text>{rollCall.location}</Text>
      </Text>

      {Timestamp.EpochNow().before(rollCall.proposedEnd) ? (
        <Text style={Typography.paragraph}>
          {STRINGS.general_ending} <ReactTimeago date={rollCall.proposedEnd.toDate()} />
        </Text>
      ) : (
        <Text style={Typography.paragraph}>{STRINGS.general_ending_now}</Text>
      )}

      {rollCall.description && (
        <CollapsibleContainer title={STRINGS.roll_call_description} isInitiallyOpen={false}>
          <Text style={Typography.paragraph}>{rollCall.description}</Text>
        </CollapsibleContainer>
      )}

      {isOrganizer && (!scannedPopTokens || scannedPopTokens.length === 0) && (
        <Text style={Typography.paragraph}>{STRINGS.roll_call_open_organizer}</Text>
      )}

      {!isOrganizer && (
        <>
          <Text style={Typography.paragraph}>{STRINGS.roll_call_open_attendee}</Text>
          <QRCode value={ScannablePopToken.encodePopToken({ pop_token: popToken })} />
        </>
      )}

      {scannedPopTokens && <AttendeeList popTokens={scannedPopTokens} />}
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  laoId: PropTypes.instanceOf(Hash).isRequired,
  // pop tokens scanned by the organizer
  scannedPopTokens: PropTypes.arrayOf(PropTypes.string.isRequired),
  isOrganizer: PropTypes.bool,
};
RollCallOpen.propTypes = propTypes;

RollCallOpen.defaultProps = {
  isOrganizer: false,
  scannedPopTokens: [],
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallOpen;
