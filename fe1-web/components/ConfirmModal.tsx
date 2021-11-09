import React from 'react';
import PropTypes from 'prop-types';
import {
  StyleSheet, View, ViewStyle, Modal, TextStyle, Text
} from 'react-native';
import STRINGS from 'res/strings';
import { white } from 'styles/colors';
import { Typography } from 'styles';
import WideButtonView from './WideButtonView';

const styles = StyleSheet.create({
  modalView: {
    backgroundColor: white,
    borderRadius: 10,
    borderWidth: 1,
    margin: 'auto',
    width: 550,
  } as ViewStyle,
  titleView: {
    borderBottomWidth: 1,
  } as ViewStyle,
  modalTitle: {
    ...Typography.important,
    alignSelf: 'flex-start',
    padding: 20,
    paddingLeft: 10,
  } as TextStyle,
  modalDescription: {
    ...Typography.base,
    fontSize: 20,
    alignSelf: 'flex-start',
    textAlign: 'left',
    padding: 20,
    paddingLeft: 10,
  } as TextStyle,
  buttonView: {
    alignSelf: 'center',
    flexDirection: 'row',
    paddingBottom: 20,
  } as ViewStyle,
});

const ConfirmModal = (props: IPropTypes) => {
  const { visibility } = props;
  const { setVisibility } = props;
  const { title } = props;
  const { description } = props;
  const { buttonConfirmText } = props;
  const { buttonCancelText } = props;
  const { onConfirmPress } = props;

  return (
    <Modal
      visible={visibility}
      onRequestClose={() => setVisibility(!visibility)}
      transparent
    >
      <View style={styles.modalView}>
        <View style={styles.titleView}>
          <Text style={styles.modalTitle}>{title}</Text>
        </View>
        <Text style={styles.modalDescription}>{description}</Text>
        <View style={styles.buttonView}>
          <WideButtonView
            title={buttonConfirmText}
            onPress={() => onConfirmPress()}
          />
          <WideButtonView
            title={buttonCancelText}
            onPress={() => setVisibility(!visibility)}
          />
        </View>
      </View>
    </Modal>
  );
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
  setVisibility: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  buttonCancelText: PropTypes.string,
  buttonConfirmText: PropTypes.string,
  onConfirmPress: PropTypes.func.isRequired,
};

ConfirmModal.propTypes = propTypes;

ConfirmModal.defaultProps = {
  buttonCancelText: STRINGS.general_button_cancel,
  buttonConfirmText: STRINGS.general_button_confirm,
};

type IPropTypes = {
  visibility: boolean,
  setVisibility: Function,
  title: string,
  description: string,
  buttonCancelText: string,
  buttonConfirmText: string,
  onConfirmPress: Function,
};

export default ConfirmModal;
