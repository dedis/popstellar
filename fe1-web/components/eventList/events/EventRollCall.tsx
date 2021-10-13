import React, { useState } from 'react';
import { Text } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { makeCurrentLao } from 'store';
import { requestOpenRollCall } from 'network';
import {
  RollCall, RollCallStatus, Wallet,
} from 'model/objects';
import QRCode from 'components/QRCode';
import WideButtonView from 'components/WideButtonView';
import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import { useNavigation } from '@react-navigation/native';

/**
 * Component used to display a RollCall event in the LAO event list
 *
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
      requestOpenRollCall(event.id).then(() => {
        navigation.navigate(STRINGS.roll_call_open);
      }).catch(
        (e) => console.debug('Unable to send Roll call open request', e),
      );
    }
  };

  const onCloseRollCall = () => {
    console.log('Closing Roll Call not yet implemented');
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
            <Text>Open</Text>
            {isOrganizer && (
              <>
                <Text>Scan the tokens</Text>
                <WideButtonView title="Close Roll Call" onPress={onCloseRollCall} />
              </>
            )}
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
            {console.log('attendees are: ', rollCallFromStore.attendees)}
            <Text>Attendees are:</Text>
            {rollCallFromStore.attendees.map((attendee: string) => (
              <Text>{attendee}</Text>
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
