import React from 'react';
import PropTypes from 'prop-types';
import {
  StyleSheet, View, ViewStyle, Modal, TextStyle, Text,
} from 'react-native';
import STRINGS from 'res/strings';
import { white } from 'styles/colors';
import { Views, Typography } from 'styles';
import WideButtonView from './WideButtonView';

const styles = StyleSheet.create({
  modalView: {
    backgroundColor: white,
    borderRadius: 10,
    borderWidth: 1,
    margin: 'auto',
    width: 600,
  } as ViewStyle,
  modalTitle: {
    ...Typography.important,
    alignSelf: 'flex-start',
  } as TextStyle,
  modalDescription: {
    ...Typography.base,
    alignSelf: 'flex-start',
  } as TextStyle,
  buttonView: {
    alignSelf: 'flex-end',
  } as ViewStyle,
});

const DismissModal = (props: IPropTypes) => {
  const { visibility } = props;
  const { setVisibility } = props;
  const { title } = props;
  const { description } = props;
  const { buttonText } = props;

  return (
    <Modal
      visible={visibility}
      onRequestClose={() => setVisibility(!visibility)}
      transparent
    >
      <View style={styles.modalView}>
        <Text style={styles.modalTitle}>{title}</Text>
        <Text style={styles.modalDescription}>{description}</Text>
        <View style={styles.buttonView}>
          <WideButtonView
            title={buttonText}
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
  buttonText: PropTypes.string,
};

DismissModal.propTypes = propTypes;

DismissModal.defaultProps = {
  buttonText: STRINGS.general_button_ok,
};

type IPropTypes = {
  visibility: boolean,
  setVisibility: Function,
  title: string,
  description: string,
  buttonText: string,
};

export default DismissModal;
