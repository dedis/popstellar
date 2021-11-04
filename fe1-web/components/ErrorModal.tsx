import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  StyleSheet, View, ViewStyle, Modal,
} from 'react-native';
import STRINGS from 'res/strings';
import { white } from 'styles/colors';
import { Views } from 'styles';
import TextBlock from './TextBlock';
import WideButtonView from './WideButtonView';

const styles = StyleSheet.create({
  modalView: {
    ...Views.base,
    backgroundColor: white,
    borderRadius: 10,
    borderWidth: 1,
    margin: 'auto',
    height: 200,
    width: 600,
  } as ViewStyle,
});

const ErrorModal = (props: IPropTypes) => {
  const { title } = props;
  const { description } = props;
  const { buttonText } = props;

  const [modalIsVisible, setModalIsVisible] = useState(false);

  return (
    <Modal
      visible={modalIsVisible}
      onRequestClose={() => setModalIsVisible(!modalIsVisible)}
      transparent
    >
      <View style={styles.modalView}>
        <TextBlock text={title} bold />
        <TextBlock text={description} />
        <WideButtonView
          title={buttonText}
          onPress={() => setModalIsVisible(!modalIsVisible)}
        />
      </View>
    </Modal>
  );
};

const propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  buttonText: PropTypes.string,
};

ErrorModal.propTypes = propTypes;

ErrorModal.defaultProps = {
  buttonText: STRINGS.general_button_ok,
};

type IPropTypes = {
  title: string,
  description: string,
  buttonText: string,
};

export default ErrorModal;
