import React, { useEffect, useMemo, useState } from 'react';
import { Text } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { useNavigation } from '@react-navigation/native';

import { QRCode, WideButtonView } from 'core/components';
import { makeEventGetter } from 'features/events/reducer';
import { makeCurrentLao } from 'features/lao/reducer';
import * as Wallet from 'features/wallet/objects';
import STRINGS from 'resources/strings';

import { requestOpenRollCall, requestReopenRollCall } from '../network';
import { RollCall, RollCallStatus } from '../objects';

/**
 * Component used to display a RollCall event in the LAO event list
 */
const EventRollCall = (props: IPropTypes) => {
  const { event } = props;
  const { isOrganizer } = props;
  const laoSelect = useMemo(makeCurrentLao, []);
  const lao = useSelector(laoSelect);
  // FIXME: use a more specific navigation
  const navigation = useNavigation<any>();

  const rollCallSelect = useMemo(() => makeEventGetter(lao?.id, event?.id), [lao, event]);
  const rollCall = useSelector(rollCallSelect) as RollCall | undefined;

  if (!lao) {
    throw new Error('no LAO is currently active');
  }
  const [popToken, setPopToken] = useState('');

  useEffect(() => {
    if (!lao || !lao.id || !rollCall || !rollCall.id) {
      return;
    }

    // Here we get the pop-token to display in the QR code
    Wallet.generateToken(lao.id, rollCall.id)
      .then((token) => setPopToken(token.publicKey.valueOf()))
      .catch((err) => console.error(`Could not generate token: ${err}`));
  }, [lao, rollCall]);

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
      requestReopenRollCall(event.idAlias).catch((e) =>
        console.debug('Unable to send Roll call re-open request', e),
      );
    } else {
      requestOpenRollCall(event.id).catch((e) =>
        console.debug('Unable to send Roll call open request', e),
      );
    }
  };

  // Scanning attendees should be available only when the Roll Call is in state opened or reopened => idAlias is defined
  const onScanAttendees = () => {
    if (!event.idAlias) {
      console.error('Unable to scan attendees, the event does not have an idAlias');
      return;
    }
    navigation.navigate(STRINGS.roll_call_open, {
      rollCallID: event.idAlias.toString(),
    });
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
              <WideButtonView title="Scan Attendees" onPress={() => onScanAttendees()} />
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
