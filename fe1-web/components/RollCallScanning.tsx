import React, { useState } from 'react';
import {
  StyleSheet, View, Text, Button, Modal, TouchableOpacity,
  Platform,
} from 'react-native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import STRINGS from 'res/strings';
import { Buttons, Colors, Typography } from '../styles';
import CameraButton from './CameraButton';
import PROPS_TYPE from '../res/Props';
import { requestCloseRollCall } from '../websockets/MessageApi';

/**
 * Scanning roll-call component: a description string, the number of participants scanned,
 * a camera view, a camera button and a closed button
 *
 * The camera button fakes the confirmation of a new attendee scan
 * The closed button ask for a confirmation to close the roll-call
 *
 * TODO add participant to the organizer server when a QR code is scan
 * TODO show confirmation of a good scan or say if an attendee is scan more than one time
 * TODO implement the camera view
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

// eslint-disable-next-line camelcase
const RollCallScanning = ({ navigation, dispatch, roll_call_id }) => {
  React.useEffect(
    () => navigation.addListener('beforeRemove', (e) => {
      e.preventDefault();
    }),
    [navigation],
  );

  const [participants, setParticipants] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);

  const closeRollCall = () => {
    const action = { type: 'SET_OPEN_ROLL_CALL_ID', value: -1 };
    dispatch(action);
    requestCloseRollCall(roll_call_id, participants);
    navigation.goBack();
  };

  const closeRollCallConfirmation = () => {
    if (Platform.OS === 'web') {
      // eslint-disable-next-line no-alert, no-undef
      if (window.confirm(STRINGS.roll_call_scan_confirmation)) {
        closeRollCall();
      }
    } else {
      setModalVisible(true);
    }
  };

  return (
    <View style={styles.container}>
      {Platform.OS !== 'web'
      && (
      <Modal
        animationType="fade"
        transparent
        visible={modalVisible}
        onRequestClose={() => {}}
      >
        <View style={styles.centeredView}>
          <View style={styles.modalView}>
            <Text style={styles.modalText}>{STRINGS.roll_call_scan_close_confirmation}</Text>
            <View style={{ flexDirection: 'row' }}>
              <TouchableOpacity
                style={{ ...styles.openButton, backgroundColor: Colors.blue }}
                onPress={() => {
                  setModalVisible(!modalVisible);
                }}
              >
                <Text style={styles.textStyle}>{STRINGS.general_no}</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={{ ...styles.openButton, backgroundColor: Colors.blue }}
                onPress={() => {
                  setModalVisible(!modalVisible);
                  closeRollCall();
                }}
              >
                <Text style={styles.textStyle}>{STRINGS.general_yes}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
      )}
      <Text style={styles.text}>
        {STRINGS.roll_call_scan_description}
      </Text>
      <Text style={styles.text}>
        {participants.length}
        {' '}
        {STRINGS.roll_call_scan_participant}
      </Text>
      <CameraButton action={() => { setParticipants([...participants, participants.length]); }} />
      <View>
        <Button
          style={styles.buttons}
          title={STRINGS.roll_call_scan_close}
          onPress={() => closeRollCallConfirmation()}
        />
      </View>
    </View>
  );
};

RollCallScanning.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
  dispatch: PropTypes.func.isRequired,
  roll_call_id: PropTypes.string.isRequired,
};

const mapStateToProps = (state) => ({
  roll_call_id: state.openRollCallIDReducer.roll_call_id,
});

export default connect(mapStateToProps)(RollCallScanning);
