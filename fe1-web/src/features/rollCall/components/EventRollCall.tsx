import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';
import React, { useEffect, useState, FunctionComponent } from 'react';
import { Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { QRCode, WideButtonView } from 'core/components';
import { selectCurrentLao } from 'features/lao/reducer';
import * as Wallet from 'features/wallet/objects';
import { WalletStore } from 'features/wallet/store';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { requestOpenRollCall, requestReopenRollCall } from '../network';
import { RollCall, RollCallStatus } from '../objects';

/**
 * Component used to display a RollCall event in the LAO event list
 */
const EventRollCall = (props: IPropTypes) => {
  const { event: rollCall } = props;
  const { isOrganizer } = props;
  const lao = useSelector(selectCurrentLao);
  // FIXME: use a more specific navigation
  const navigation = useNavigation<any>();
  const toast = useToast();

  if (!lao) {
    throw new Error('no LAO is currently active');
  }
  const [popToken, setPopToken] = useState('');
  const [hasWalletBeenInitialized, setHasWalletBeenInitialized] = useState(WalletStore.hasSeed());

  // re-check if wallet has been initialized after focus events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      setHasWalletBeenInitialized(WalletStore.hasSeed());
    });
  }, [navigation]);

  // Once the roll call is opened the first time, idAlias is defined, and needed for closing/reopening the roll call
  const eventHasBeenOpened = rollCall.idAlias !== undefined;

  useEffect(() => {
    if (!lao?.id || !rollCall?.id) {
      return;
    }

    // Here we get the pop-token to display in the QR code
    Wallet.generateToken(lao.id, rollCall.id)
      .then((token) => setPopToken(token.publicKey.valueOf()))
      .catch((err) => console.error(`Could not generate token: ${err}`));
  }, [hasWalletBeenInitialized, lao, rollCall]);

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
    requestOpenRollCall(rollCall.id).catch((e) => {
      makeToastErr('Unable to send roll call open request');
      console.debug('Unable to send Roll call open request', e);
    });
  };

  const onReopenRollCall = () => {
    if (eventHasBeenOpened) {
      requestReopenRollCall(rollCall.idAlias).catch((e) => {
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
      navigation.navigate(STRINGS.roll_call_open, {
        rollCallID: rollCall.idAlias.toString(),
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
              <WideButtonView title={STRINGS.roll_call_open} onPress={() => onOpenRollCall()} />
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
              <WideButtonView
                title={STRINGS.roll_call_scan_attendees}
                onPress={() => onScanAttendees()}
              />
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
              <WideButtonView title={STRINGS.roll_call_reopen} onPress={() => onReopenRollCall()} />
            )}
          </>
        );
      default:
        console.warn('Roll Call Status was undefined in EventRollCall');
        return null;
    }
  };

  return (
    <>
      <Text>Roll Call</Text>
      {getRollCallDisplay(rollCall.status)}
    </>
  );
};

const propTypes = {
  event: PropTypes.instanceOf(RollCall).isRequired,
  isOrganizer: PropTypes.bool,
};
EventRollCall.propTypes = propTypes;

EventRollCall.defaultProps = {
  isOrganizer: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventRollCall;

export const RollCallEventTypeComponent = {
  isOfType: (event: unknown) => event instanceof RollCall,
  Component: EventRollCall as FunctionComponent<{
    event: unknown;
    isOrganizer: boolean | null | undefined;
  }>,
};
