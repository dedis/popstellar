import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState, useMemo } from 'react';
import { Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { PoPIcon, QRCode } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ActionSheetOption, useActionSheet } from 'core/hooks/ActionSheet';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { PublicKey } from 'core/objects';
import { Color, Icon, List, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
import { requestCloseRollCall, requestOpenRollCall, requestReopenRollCall } from '../network';
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

  const navigation = useNavigation<NavigationProps['navigation']>();

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

  const getRollCallDisplay = (status: RollCallStatus) => {
    switch (status) {
      case RollCallStatus.CREATED:
        return (
          <>
            <Text style={Typography.paragraph}>
              <Text style={[Typography.base, Typography.important]}>
                {STRINGS.general_starting_at}
              </Text>
              {'\n'}
              <Text>
                {rollCall.proposedStart.toDate().toLocaleDateString()}{' '}
                {rollCall.proposedStart.toDate().toLocaleTimeString()}
              </Text>
            </Text>

            <Text style={Typography.paragraph}>
              <Text style={[Typography.base, Typography.important]}>
                {STRINGS.general_ending_at}
              </Text>
              {'\n'}
              <Text>
                {rollCall.proposedEnd.toDate().toLocaleDateString()}{' '}
                {rollCall.proposedEnd.toDate().toLocaleTimeString()}
              </Text>
            </Text>
          </>
        );
      case RollCallStatus.REOPENED:
      case RollCallStatus.OPENED:
        if (isOrganizer) {
          if (!attendeePopTokens) {
            return <Text style={Typography.paragraph}>{STRINGS.roll_call_open_organizer}</Text>;
          }

          return null;
        }

        return (
          <>
            <Text style={Typography.paragraph}>{STRINGS.roll_call_open_attendee}</Text>
            <QRCode visibility value={popToken} />
          </>
        );

      case RollCallStatus.CLOSED:
        return (
          <>
            <Text style={Typography.paragraph}>
              <Text style={[Typography.base, Typography.important]}>
                {STRINGS.general_ended_at}
              </Text>
              {'\n'}
              <Text>
                {rollCall.closedAt?.toDate().toLocaleDateString()}{' '}
                {rollCall.closedAt?.toDate().toLocaleTimeString()}
              </Text>
            </Text>
          </>
        );
      default:
        console.warn('Roll Call Status was undefined in EventRollCall');
        return null;
    }
  };

  const popTokens = rollCall.attendees || attendeePopTokens;

  return (
    <ScreenWrapper>
      {rollCall.description && (
        <Text style={Typography.paragraph}>
          <Text style={[Typography.base, Typography.important]}>
            {STRINGS.roll_call_description}
          </Text>
          {'\n'}
          <Text>{rollCall.description}</Text>
        </Text>
      )}

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.roll_call_location}</Text>
        {'\n'}
        <Text>{rollCall.location}</Text>
      </Text>

      {getRollCallDisplay(rollCall.status)}

      {popTokens && (
        <View style={List.container}>
          <ListItem.Accordion
            containerStyle={List.accordionItem}
            content={
              <ListItem.Content>
                <ListItem.Title style={[Typography.base, Typography.important]}>
                  Attendees
                </ListItem.Title>
              </ListItem.Content>
            }
            isExpanded>
            {popTokens.map((token, idx) => {
              const listStyle = List.getListItemStyles(idx === 0, idx === popTokens.length - 1);

              return (
                <ListItem key={token.valueOf()} containerStyle={listStyle} style={listStyle}>
                  <View style={List.icon}>
                    <PoPIcon name="qrCode" color={Color.primary} size={Icon.size} />
                  </View>
                  <ListItem.Content>
                    <ListItem.Title style={Typography.base}>{token.valueOf()}</ListItem.Title>
                  </ListItem.Content>
                </ListItem>
              );
            })}
          </ListItem.Accordion>
        </View>
      )}
    </ScreenWrapper>
  );
};

export default ViewSingleRollCall;

/**
 * Component rendered in the top middle of the navgiation bar when looking
 * at a single roll call. Makes sure it shows the name of the roll call and
 * not just some static string.
 */
export const ViewSingleRollCallScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: rollCallId } = route.params;

  const selectRollCall = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(selectRollCall);

  if (!rollCall) {
    throw new Error(`Could not find a roll call with id ${rollCallId}`);
  }

  return <Text style={Typography.topNavigationHeading}>{rollCall.name}</Text>;
};

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

  const laoId = RollCallHooks.useCurrentLaoId();
  if (!laoId) {
    throw new Error('no LAO is currently active');
  }

  // don't show a button for non-organizers
  if (!isOrganizer) {
    return null;
  }

  // Once the roll call is opened the first time, idAlias is defined, and needed for closing/reopening the roll call
  const eventHasBeenOpened = rollCall.idAlias !== undefined;

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
    if (eventHasBeenOpened) {
      requestReopenRollCall(laoId, rollCall.idAlias).catch((e) => {
        makeToastErr(STRINGS.roll_call_location_error_reopen_roll_call);
        console.debug(STRINGS.roll_call_location_error_reopen_roll_call, e);
      });
    } else {
      makeToastErr(STRINGS.roll_call_location_error_reopen_roll_call_no_alias);
      console.debug(STRINGS.roll_call_location_error_reopen_roll_call_no_alias);
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
      makeToastErr(STRINGS.roll_call_location_error_scanning_no_alias);
      console.debug(STRINGS.roll_call_location_error_scanning_no_alias);
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
        return [{ displayName: STRINGS.roll_call_open, action: onOpenRollCall }];
      case RollCallStatus.OPENED:
      case RollCallStatus.REOPENED:
        return [
          { displayName: STRINGS.roll_call_scan_attendees, action: onScanAttendees },
          { displayName: STRINGS.roll_call_close, action: onCloseRollCall },
        ];
      case RollCallStatus.CLOSED:
        return [{ displayName: STRINGS.roll_call_reopen, action: onReopenRollCall }];

      default:
        throw new Error(`Unkwon roll call status '${status}'`);
    }
  };

  return (
    <PoPTouchableOpacity
      onPress={() => showActionSheet(getActionOptions(rollCall.status))}
      testID="roll-call-options">
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

export const ViewSingleRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_view_single_roll_call,
  Component: ViewSingleRollCall,
  headerTitle: ViewSingleRollCallScreenHeader,
  headerRight: ViewSinglRollCallScreenRightHeader,
};
