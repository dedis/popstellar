import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState, useMemo } from 'react';
import { Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { QRCode, Button } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
import { requestOpenRollCall, requestReopenRollCall } from '../network';
import { RollCallStatus } from '../objects';
import { makeRollCallSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_view_single_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const ViewSingleRollCall = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: rollCallId, isOrganizer } = route.params;

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);

  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  const navigation = useNavigation<NavigationProps['navigation']>();

  const toast = useToast();

  const laoId = RollCallHooks.useCurrentLaoId();
  const generateToken = RollCallHooks.useGenerateToken();
  const hasSeed = RollCallHooks.useHasSeed();

  if (!laoId) {
    throw new Error('no LAO is currently active');
  }
  const [popToken, setPopToken] = useState('');
  const [hasWalletBeenInitialized, setHasWalletBeenInitialized] = useState(hasSeed());

  // re-check if wallet has been initialized after focus events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      setHasWalletBeenInitialized(hasSeed());
    });
  }, [navigation, hasSeed]);

  // Once the roll call is opened the first time, idAlias is defined, and needed for closing/reopening the roll call
  const eventHasBeenOpened = rollCall.idAlias !== undefined;

  useEffect(() => {
    if (!hasWalletBeenInitialized || !laoId || !rollCall?.id) {
      return;
    }

    // Here we get the pop-token to display in the QR code
    generateToken(laoId, rollCall.id)
      .then((token) => setPopToken(token.publicKey.valueOf()))
      .catch((err) => console.error(`Could not generate token: ${err}`));
  }, [hasWalletBeenInitialized, generateToken, laoId, rollCall]);

  if (!rollCall) {
    console.debug('Error in Roll Call display: Roll Call doesnt exist in store');
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
      makeToastErr('Unable to send roll call open request');
      console.debug('Unable to send Roll call open request', e);
    });
  };

  const onReopenRollCall = () => {
    if (eventHasBeenOpened) {
      requestReopenRollCall(laoId, rollCall.idAlias).catch((e) => {
        makeToastErr('Unable to send Roll call re-open request');
        console.debug('Unable to send Roll call re-open request', e);
      });
    } else {
      makeToastErr('Unable to send roll call re-open request, the event does not have an idAlias');
      console.debug('Unable to send roll call re-open request, the event does not have an idAlias');
    }
  };

  const onScanAttendees = () => {
    if (eventHasBeenOpened) {
      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_events,
        params: {
          screen: STRINGS.navigation_lao_events_open_roll_call,
          params: { rollCallId: rollCall.id.toString() },
        },
      });
    } else {
      makeToastErr('Unable to scan attendees, the event does not have an idAlias');
      console.debug('Unable to scan attendees, the event does not have an idAlias');
    }
  };

  const getRollCallDisplay = (status: RollCallStatus) => {
    switch (status) {
      case RollCallStatus.CREATED:
        return (
          <>
            <Text>Not Open yet</Text>
            <Text>Be sure to have set up your Wallet</Text>
            {isOrganizer && (
              <Button onPress={onOpenRollCall}>
                <Text style={[Typography.base, Typography.centered, Typography.negative]}>
                  {STRINGS.roll_call_open}
                </Text>
              </Button>
            )}
          </>
        );
      case RollCallStatus.REOPENED:
      case RollCallStatus.OPENED:
        return (
          <>
            {!isOrganizer && (
              <>
                <Text>Let the organizer scan your Pop token</Text>
                <QRCode visibility value={popToken} />
              </>
            )}
            {isOrganizer && (
              <Button onPress={onScanAttendees}>
                <Text style={[Typography.base, Typography.centered, Typography.negative]}>
                  {STRINGS.roll_call_scan_attendees}
                </Text>
              </Button>
            )}
          </>
        );
      case RollCallStatus.CLOSED:
        return (
          <>
            <Text>Closed</Text>
            <Text>Attendees are:</Text>
            {rollCall.attendees?.map((attendee) => (
              <Text key={attendee.valueOf()}>{attendee}</Text>
            ))}
            {isOrganizer && (
              <Button onPress={onReopenRollCall}>
                <Text style={[Typography.base, Typography.centered, Typography.negative]}>
                  {STRINGS.roll_call_reopen}
                </Text>
              </Button>
            )}
          </>
        );
      default:
        console.warn('Roll Call Status was undefined in EventRollCall');
        return null;
    }
  };

  return (
    <ScreenWrapper>
      <Text>Roll Call</Text>
      {getRollCallDisplay(rollCall.status)}
    </ScreenWrapper>
  );
};

export default ViewSingleRollCall;

export const ViewSingleMeetingScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: rollCallId } = route.params;

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);

  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  return <Text style={Typography.topNavigationHeading}>{rollCall.name}</Text>;
};

export const ViewSingleRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_view_single_roll_call,
  Component: ViewSingleRollCall,
  headerTitle: ViewSingleMeetingScreenHeader,
};
