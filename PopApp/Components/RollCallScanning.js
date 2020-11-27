import React, { useState } from 'react';
import {
  StyleSheet, View, Text, Button, Modal, TouchableOpacity,
} from 'react-native';

import { Buttons, Colors, Typography } from '../Styles';
import CameraButton from './CameraButton';
import PROPS_TYPE from '../res/Props';
import STRINGS from '../res/strings';

/**
* Scanning roll-call component
*
* In the future will scan a QR code and register participant, not just a dummy button
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-evenly',
  },
  text: {
    ...Typography.base,
  },
  buttons: {
    ...Buttons.base,
  },
  centeredView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 22,
  },
  modalView: {
    margin: 20,
    backgroundColor: 'white',
    borderRadius: 20,
    padding: 35,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  openButton: {
    backgroundColor: '#F194FF',
    borderRadius: 20,
    padding: 10,
    elevation: 2,
  },
  textStyle: {
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
  },
  modalText: {
    marginBottom: 15,
    textAlign: 'center',
  },
});

const RollCallScanning = ({ navigation }) => {
  const [nbPartipants, setNbPartipant] = useState(0);
  const [modalVisible, setModalVisible] = useState(false);

  return (
    <View style={styles.container}>
      <Modal
        animationType="fade"
        transparent
        visible={modalVisible}
        onRequestClose={() => {}}
      >
        <View style={styles.centeredView}>
          <View style={styles.modalView}>
            <Text style={styles.modalText}>Do you confirm to close the roll-call ?</Text>
            <View style={{ flexDirection: 'row' }}>
              <TouchableOpacity
                style={{ ...styles.openButton, backgroundColor: Colors.blue }}
                onPress={() => {
                  setModalVisible(!modalVisible);
                }}
              >
                <Text style={styles.textStyle}>No</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={{ ...styles.openButton, backgroundColor: Colors.blue }}
                onPress={() => {
                  setModalVisible(!modalVisible);
                  navigation.goBack();
                }}
              >
                <Text style={styles.textStyle}>Yes</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
      <Text style={styles.text}>
        {STRINGS.roll_call_scan_description}
      </Text>
      <Text style={styles.text}>
        {nbPartipants}
        {' '}
        {STRINGS.roll_call_scan_participant}
      </Text>
      <CameraButton action={() => { setNbPartipant(nbPartipants + 1); }} />
      <View>
        <Button
          style={styles.buttons}
          title={STRINGS.roll_call_scan_close}
          onPress={() => setModalVisible(true)}
        />
      </View>
    </View>
  );
};

RollCallScanning.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default RollCallScanning;
