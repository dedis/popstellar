import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as Clipboard from 'expo-clipboard';
import PropTypes from 'prop-types';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import ReactTimeago from 'react-timeago';

import { CollapsibleContainer, PoPTextButton, QRCode } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import { ScannablePopToken } from 'core/objects/ScannablePopToken';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { requestCloseRollCall } from '../network';
import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.events_view_single_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const textStyle = StyleSheet.create({
  topSpace: {
    marginTop: Spacing.x2,
  } as ViewStyle,
});

const dedupAttendees = (attendees: PublicKey[]): PublicKey[] => {
  const set = new Set(attendees.map((attendee) => attendee.valueOf()));
  return [...set].map((attendee) => PublicKey.fromState(attendee));
};

const RollCallOpen = ({
  rollCall,
  laoId,
  isConnected,
  isOrganizer,
  scannedPopTokens,
}: IPropTypes) => {
  const generateToken = RollCallHooks.useGenerateToken();
  const hasSeed = RollCallHooks.useHasSeed();
  const toast = useToast();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const [popToken, setPopToken] = useState('');
  const [hasWalletBeenInitialized, setHasWalletBeenInitialized] = useState(hasSeed());
  const allAttendees = useMemo(() => {
    const othersWithDuplicates = [...(rollCall.attendees || []), ...(scannedPopTokens || [])];
    const allWithDuplicates =
      popToken === '' || !isOrganizer
        ? othersWithDuplicates
        : [...othersWithDuplicates, PublicKey.fromState(popToken)];
    return dedupAttendees(allWithDuplicates);
  }, [isOrganizer, popToken, rollCall.attendees, scannedPopTokens]);

  const onAddAttendees = useCallback(() => {
    // Once the roll call is opened the first time, idAlias is defined
    if (!rollCall.idAlias) {
      toast.show(STRINGS.roll_call_error_scanning_no_alias, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.warn(STRINGS.roll_call_error_scanning_no_alias);
      return;
    }

    navigation.navigate(STRINGS.navigation_app_lao, {
      screen: STRINGS.navigation_lao_events,
      params: {
        screen: STRINGS.events_open_roll_call,
        params: {
          rollCallId: rollCall.id.toString(),
          attendeePopTokens: allAttendees.map((e) => e.valueOf()),
        },
      },
    });
  }, [toast, navigation, rollCall, allAttendees]);

  const onCloseRollCall = useCallback(async () => {
    if (!rollCall.idAlias) {
      toast.show(STRINGS.roll_call_error_close_roll_call_no_alias, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.warn(STRINGS.roll_call_error_close_roll_call_no_alias);
      return;
    }

    try {
      await requestCloseRollCall(laoId, rollCall.idAlias, allAttendees);
      navigation.navigate(STRINGS.navigation_lao_events_home);
    } catch (err) {
      console.log(err);
      toast.show(STRINGS.roll_call_error_close_roll_call, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    }
  }, [toast, navigation, rollCall, laoId, allAttendees]);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    if (!isOrganizer) {
      return [];
    }

    return [
      {
        title: STRINGS.roll_call_close,
        onPress: onCloseRollCall,
        buttonStyle: 'secondary',
        disabled: isConnected !== true,
      },
      {
        title: STRINGS.roll_call_scan_attendees,
        onPress: onAddAttendees,
        disabled: isConnected !== true,
      },
    ] as ToolbarItem[];
  }, [isConnected, isOrganizer, onCloseRollCall, onAddAttendees]);

  // re-check if wallet has been initialized after focus events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      setHasWalletBeenInitialized(hasSeed());
    });
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
          {STRINGS.general_ending} <ReactTimeago live date={rollCall.proposedEnd.toDate()} />
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
          <View>
            <QRCode
              value={ScannablePopToken.encodePopToken({ pop_token: popToken })}
              overlayText={STRINGS.roll_call_qrcode_text}
            />
            <Text
              style={[
                Typography.paragraph,
                Typography.centered,
                Typography.code,
                textStyle.topSpace,
              ]}>
              {popToken}
            </Text>
            <PoPTextButton onPress={() => Clipboard.setStringAsync(popToken)}>
              {STRINGS.general_copy}
            </PoPTextButton>
          </View>
        </>
      )}

      {scannedPopTokens && <AttendeeList popTokens={allAttendees} personalToken={popToken} />}
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  laoId: PropTypes.instanceOf(Hash).isRequired,
  // pop tokens scanned by the organizer
  scannedPopTokens: PropTypes.arrayOf(PropTypes.instanceOf(PublicKey).isRequired),
  isConnected: PropTypes.bool,
  isOrganizer: PropTypes.bool,
};
RollCallOpen.propTypes = propTypes;

RollCallOpen.defaultProps = {
  isConnected: undefined,
  isOrganizer: false,
  scannedPopTokens: [],
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallOpen;
