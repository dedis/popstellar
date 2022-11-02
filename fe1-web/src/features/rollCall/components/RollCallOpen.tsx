import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import ReactTimeago from 'react-timeago';

import { CollapsibleContainer, QRCode } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { PublicKey } from 'core/objects';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { RollCall } from '../objects';
import AttendeeList from './AttendeeList';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_view_single_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const RollCallOpen = ({ rollCall, isOrganizer, scannedPopTokens }: IPropTypes) => {
  const laoId = RollCallHooks.useAssertCurrentLaoId();
  const generateToken = RollCallHooks.useGenerateToken();
  const hasSeed = RollCallHooks.useHasSeed();
  const toast = useToast();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const [popToken, setPopToken] = useState('');
  const [hasWalletBeenInitialized, setHasWalletBeenInitialized] = useState(hasSeed());

  const onAddAttendees = () => {
    // Once the roll call is opened the first time, idAlias is defined
    if (rollCall.idAlias) {
      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_events,
        params: {
          screen: STRINGS.navigation_lao_events_open_roll_call,
          params: {
            rollCallId: rollCall.id.toString(),
            initialAttendeePopTokens: (scannedPopTokens || []).map((e) => e.valueOf()),
          },
        },
      });
    } else {
      toast.show(STRINGS.roll_call_location_error_scanning_no_alias, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.debug(STRINGS.roll_call_location_error_scanning_no_alias);
    }
  };

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
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{rollCall.name}</Text>
        {'\n'}
        <Text>{rollCall.location}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        {STRINGS.general_ending} <ReactTimeago date={rollCall.proposedEnd.toDate()} />
      </Text>

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
          <QRCode value={popToken} />
        </>
      )}

      {scannedPopTokens && (
        <AttendeeList
          popTokens={scannedPopTokens}
          isOrganizer={isOrganizer}
          onAddAttendee={onAddAttendees}
        />
      )}
    </ScreenWrapper>
  );
};

const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
  // pop tokens scanned by the organizer
  scannedPopTokens: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.instanceOf(PublicKey).isRequired).isRequired,
    PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  ]),
  isOrganizer: PropTypes.bool,
};
RollCallOpen.propTypes = propTypes;

RollCallOpen.defaultProps = {
  isOrganizer: false,
  scannedPopTokens: [],
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallOpen;
