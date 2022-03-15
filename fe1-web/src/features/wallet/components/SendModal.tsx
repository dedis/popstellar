import React from 'react';
import { StyleSheet, ViewStyle, Modal, TextInput, View } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import PropTypes from 'prop-types';
import { PopToken } from 'core/objects';

const styles = StyleSheet.create({
  modal: {
    maxHeight: '200',
    maxWidth: '200',
    margin: 'auto',
    backgroundColor: 'lightgray',
    padding: '40px',
    borderRadius: 3,
  },
  row: {
    flex: 1,
    flexDirection: 'row',
  },
});
const SendModal = (props: IPropTypes) => {
  const { balance } = props;
  const { popToken } = props;
  const { visible } = props;
  const { setVisible } = props;
  return (
    <Modal
      animationType="fade"
      transparent
      visible={visible}
      onRequestClose={() => setVisible(false)}>
      <View style={styles.modal}>
        <TextBlock text={'Your balance = '.concat(balance)} />
        <View style={styles.row}>
          <TextBlock text="To: " />
          <TextInput defaultValue="0x000" />
          <WideButtonView title="Scan QR" onPress={() => {}} />
        </View>
        <View style={styles.row}>
          <TextBlock text="Amount: " />
          <TextInput defaultValue="0.00" />
        </View>
        <WideButtonView title="Send" onPress={() => {}} />
      </View>
    </Modal>
  );
};
const propTypes = {
  balance: PropTypes.string.isRequired,
  popToken: PropTypes.instanceOf(PopToken).isRequired,
  visible: PropTypes.bool.isRequired,
  setVisible: PropTypes.func.isRequired,
};
SendModal.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default SendModal;
