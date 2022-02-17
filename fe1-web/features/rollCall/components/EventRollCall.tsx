import React, { useState } from 'react';
import { Text } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { useNavigation } from '@react-navigation/native';

import { makeCurrentLao } from 'store';
import { Timestamp, Wallet } from 'model/objects';
import QRCode from 'components/QRCode';
import WideButtonView from 'components/WideButtonView';
import STRINGS from 'res/strings';

import { requestOpenRollCall } from '../network/RollCallMessageApi';
import { RollCall, RollCallStatus } from '../objects';

/**
 * Component used to display a RollCall event in the LAO event list
 */
const EventRollCall = (props: IPropTypes) => {
  const { event } = props;
  const { isOrganizer } = props;
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);
  const navigation = useNavigation();

  if (!lao) {
    console.warn('no LAO is currently active');
    return null;
  }
  const [popToken, setPopToken] = useState('');

  const rollCallFromStore = useSelector((state) => (
    // @ts-ignore
    state.events.byLaoId[lao.id].byId[event.id]));
  if (!rollCallFromStore) {
    console.debug('Error in Roll Call display: Roll Call doesnt exist in store');
    return null;
  }

  const onOpenRollCall = (reopen: boolean) => {
    if (reopen) {
      if (!event.idAlias) {
        console.debug('Unable to send roll call re-open request, the event does not have an idAlias');
        return;
      }
      requestOpenRollCall(event.idAlias).then().catch(
        (e) => console.debug('Unable to send Roll call re-open request', e),
      );
    } else {
      const time = Timestamp.EpochNow();
      requestOpenRollCall(event.id, time).then(() => {
        // @ts-ignore
        navigation.navigate(STRINGS.roll_call_open,
          { rollCallID: event.id.toString(), time: time.toString() });
      }).catch(
        (e) => console.debug('Unable to send Roll call open request', e),
      );
    }
  };

  // Here we get the pop-token to display in the QR code
  Wallet.generateToken(lao.id, event.id)
    .then((token) => setPopToken(token.publicKey.valueOf()));

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
          </>
        );
      case RollCallStatus.CLOSED:
        return (
          <>
            <Text>Closed</Text>
            <Text>Attendees are:</Text>
            {rollCallFromStore.attendees.map((attendee: string) => (
              <Text key={attendee}>{attendee}</Text>
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
      {getRollCallDisplay(rollCallFromStore.status)}
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
