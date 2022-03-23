import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';
import React, { useEffect, useMemo, useState, FunctionComponent } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import { QRCode, WideButtonView } from 'core/components';
import { Timestamp } from 'core/objects';
import { makeEventGetter } from 'features/events/reducer';
import { selectCurrentLao } from 'features/lao/reducer';
import * as Wallet from 'features/wallet/objects';
import { WalletStore } from 'features/wallet/store';
import STRINGS from 'resources/strings';

import { requestOpenRollCall } from '../network';
import { RollCall, RollCallStatus } from '../objects';

/**
 * Component used to display a RollCall event in the LAO event list
 */
const EventRollCall = (props: IPropTypes) => {
  const { event } = props;
  const { isOrganizer } = props;
  const lao = useSelector(selectCurrentLao);
  // FIXME: use a more specific navigation
  const navigation = useNavigation<any>();

  const rollCallSelect = useMemo(() => makeEventGetter(lao?.id, event?.id), [lao, event]);
  const rollCall = useSelector(rollCallSelect) as RollCall | undefined;

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

  const onOpenRollCall = (reopen: boolean) => {
    if (reopen) {
      if (!event.idAlias) {
        console.debug(
          'Unable to send roll call re-open request, the event does not have an idAlias',
        );
        return;
      }
      requestOpenRollCall(event.idAlias).catch((e) =>
        console.debug('Unable to send Roll call re-open request', e),
      );
    } else {
      const time = Timestamp.EpochNow();
      requestOpenRollCall(event.id, time)
        .then(() => {
          navigation.navigate(STRINGS.roll_call_open, {
            rollCallID: event.id.toString(),
            time: time.toString(),
          });
        })
        .catch((e) => console.debug('Unable to send Roll call open request', e));
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
              <WideButtonView title="Open Roll Call" onPress={() => onOpenRollCall(false)} />
            )}
          </>
        );
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
                title="Scan Attendees"
                onPress={() => console.error('not implemented yet')}
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
              <WideButtonView title="Re-open Roll Call" onPress={() => onOpenRollCall(true)} />
            )}
          </>
        );
      case RollCallStatus.REOPENED:
        return (
          <>
            <Text>Re-Opened</Text>
            <QRCode visibility value={popToken} />
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
  isOrganizer: PropTypes.bool.isRequired,
};
EventRollCall.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventRollCall;

export const RollCallEventTypeComponent = {
  isOfType: (event: unknown) => event instanceof RollCall,
  Component: EventRollCall as FunctionComponent<{
    event: unknown;
    isOrganizer: boolean | null | undefined;
  }>,
};
