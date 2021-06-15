import React from 'react';
import { FlatList, StyleSheet } from 'react-native';

import { Spacing } from 'styles';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';
import ParagraphBlock from 'components/ParagraphBlock';
import {HDWallet} from "../../../model/objects";
import {WalletStore} from "../../../store/stores/WalletStore";

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

enum RollCallStatus {
  FUTURE = 'Future',
  CLOSE = 'Close',
  OPEN = 'Open',
}

const EventRollCall = (props: IPropTypes) => {
  const { event } = props;
  const { childrenVisibility } = props;
  const { renderItemFn } = props;

  const getStatus = () => {
    let status;
    if (event.scheduled) {
      status = RollCallStatus.FUTURE;
    } else if (event.id) {
      status = RollCallStatus.CLOSE;
    } else {
      status = RollCallStatus.OPEN;
    }

    return `Status: ${status}`;
  };

  // const popToken = WalletStore.get().then((e) => HDWallet.fromState(e).then(wallet => wallet.generateToken(laoId, RCId).then(keyPair => keyPair.publicKey)));

  // const getRollCallDisplay = (status: RollCallStatus) => {
  //   switch (status){
  //     case RollCallStatus.FUTURE:
  //       return (
  //         <Text>Future</Text>
  //       )
  //   }
  // }

  return (
    <>
      <ParagraphBlock text={getStatus()} />
      <ParagraphBlock text="Participants #" />

      { getStatus() === RollCallStatus.OPEN && (
        <ParagraphBlock text="QR Code" />
      )}
      { childrenVisibility && (
        <FlatList
          data={event.children}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderItemFn}
          listKey={`RollCallEvent-${event.id.toString()}`}
          style={styles.flatList}
        />
      )}
    </>
  );
};

const propTypes = {
  event: PROPS_TYPE.roll_call.isRequired,
  childrenVisibility: PropTypes.bool.isRequired,
  renderItemFn: PropTypes.func.isRequired,
};
EventRollCall.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventRollCall;
