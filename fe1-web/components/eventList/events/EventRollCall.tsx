import React, {useState} from 'react';
import { StyleSheet, Text } from 'react-native';

import { Spacing } from 'styles';
import PropTypes from 'prop-types';
import {
  RollCall, RollCallStatus, HDWallet, PublicKey,
} from 'model/objects';
import { useSelector } from 'react-redux';
import { getStore, makeCurrentLao, WalletStore } from 'store';
import QRCode from 'components/QRCode';
import WideButtonView from '../../WideButtonView';

/**
 * Component used to display a RollCall event in the LAO event list
 *
 * TODO implement the QR code
 */
const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.xs,
  },
});

const EventRollCall = (props: IPropTypes) => {
  const { event } = props;
  const { isOrganizer } = props;
  const storeState = getStore().getState();
  const getCurrentLao = makeCurrentLao();
  const lao = getCurrentLao(storeState);
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

  const onOpenRollCall = () => {
    console.log('opening Roll Call not yet implemented');
  };
  // Here we get the pop-token to display in the QR code
  // let popToken: PublicKey;
  // WalletStore.get().then((e) => (
  //   HDWallet.fromState(e)).then((wallet) => (
  //   wallet.generateToken(lao.id, event.id)).then((keyPair) => {
  //   popToken = keyPair.publicKey;
  // })));
  //
  // let other_token: PublicKey;

  WalletStore.get().then((encryptedSeed) => {
    if (encryptedSeed !== undefined) {
      HDWallet.fromState(encryptedSeed)
        .then((wallet) => {
          wallet.generateToken(lao.id, event.id)
            .then((token) => {
              setPopToken(token.publicKey.valueOf());
              // other_token = token.publicKey;
            });
        });
    }
  });

  // const ptoken: PublicKey = WalletStore.get().then((e) => (
  //   HDWallet.fromState(e)).then((wallet) => (
  //   wallet.generateToken(lao.id, event.id)).then((keyPair) => (
  //   keyPair.publicKey))));
  //
  // console.log('pop token is: ');
  // console.log(other_token.valueOf());
  // console.log(other_token.toString());

  const getRollCallDisplay = (status: RollCallStatus) => {
    switch (status) {
      case RollCallStatus.CREATED:
        return (
          <>
            <Text>Not Open yet</Text>
            {isOrganizer && (
              <WideButtonView title="Open Roll Call" onPress={onOpenRollCall} />
            )}
          </>
        );
      case RollCallStatus.OPENED:
        return (
          <>
            <Text>Open - Let the organizer scan your Pop Token</Text>
            <QRCode visibility value={popToken} />
          </>
        );
      case RollCallStatus.CLOSED:
        return (
          <>
            <Text>Closed</Text>
          </>
        );
      case RollCallStatus.REOPENED:
        return (
          <>
            <Text>Re-Opened</Text>
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
