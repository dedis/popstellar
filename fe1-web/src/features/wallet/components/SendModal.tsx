import PropTypes from 'prop-types';
import React from 'react';
import { Button, Modal, StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { Input } from 'react-native-elements';

import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import RoundIconButton from './RoundIconButton';

const styles = StyleSheet.create({
  modal: {
    padding: 20,
    height: '80%',
  } as ViewStyle,
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
  } as ViewStyle,
  title: Typography.important as ViewStyle,
  sendContainer: {
    height: '100%',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
  } as ViewStyle,
  sendView: {
    margin: 'auto',
  } as ViewStyle,
  label: {
    color: 'gray',
    fontSize: 17,
  } as TextStyle,
  input: {
    minWidth: 300,
    padding: 5,
  } as ViewStyle,
});

const SendModal = (props: IPropTypes) => {
  const { modalVisible, setModalVisible } = props;
  const switchModalVisibility = () => setModalVisible(!modalVisible);
  return (
    <Modal animationType="slide" visible={modalVisible} onRequestClose={switchModalVisibility}>
      <View style={styles.modal}>
        <View style={containerStyles.centerWithMargin}>
          <View style={styles.topBar}>
            <Text style={styles.title}>{STRINGS.wallet_send_title}</Text>
            <RoundIconButton name="close" onClick={switchModalVisibility} />
          </View>
          <View style={styles.sendContainer}>
            <View style={styles.sendView}>
              <Input style={styles.input} label={STRINGS.wallet_send_destination_label} />
              <Input style={styles.input} label={STRINGS.wallet_send_amount_label} />
              <Button title={STRINGS.wallet_send_title} onPress={() => {}}/>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const propTypes = {
  modalVisible: PropTypes.bool.isRequired,
  setModalVisible: PropTypes.func.isRequired,
};
SendModal.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default SendModal;
