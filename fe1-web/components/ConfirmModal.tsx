import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  View, Modal, Text,
} from 'react-native';
import STRINGS from 'res/strings';
import styles from 'styles/stylesheets/modal';
import WideButtonView from './WideButtonView';
import TextInputLine from './TextInputLine';

/**
 * A modal used to ask for the confirmation or cancellation of the user.
 * It can also ask for a user input.
 */

const ConfirmModal = (props: IPropTypes) => {
  const { visibility } = props;
  const { setVisibility } = props;
  const { title } = props;
  const { description } = props;
  const { buttonConfirmText } = props;
  const { buttonCancelText } = props;
  const { onConfirmPress } = props;
  const { hasTextInput } = props;
  const { textInputPlaceholder } = props;
  const [textInput, setTextInput] = useState('');

  return (
    <Modal
      visible={visibility}
      transparent
    >
      <View style={styles.modalView}>
        <View style={styles.titleView}>
          <Text style={styles.modalTitle}>{title}</Text>
        </View>
        <Text style={styles.modalDescription}>{description}</Text>
        { hasTextInput
          ? (
            <TextInputLine
              onChangeText={(input) => setTextInput(input)}
              placeholder={textInputPlaceholder}
            />
          ) : null }
        <View style={styles.buttonView}>
          <WideButtonView
            title={buttonConfirmText}
            onPress={() => onConfirmPress(textInput)}
          />
          <WideButtonView
            title={buttonCancelText}
            onPress={() => {
              setVisibility(!visibility);
              setTextInput('');
            }}
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
  hasTextInput: PropTypes.bool,
  textInputPlaceholder: PropTypes.string,
};

ConfirmModal.propTypes = propTypes;

ConfirmModal.defaultProps = {
  buttonCancelText: STRINGS.general_button_cancel,
  buttonConfirmText: STRINGS.general_button_confirm,
  hasTextInput: false,
  textInputPlaceholder: '',
};

type IPropTypes = {
  visibility: boolean,
  setVisibility: Function,
  title: string,
  description: string,
  buttonCancelText: string,
  buttonConfirmText: string,
  onConfirmPress: Function,
  hasTextInput: boolean,
  textInputPlaceholder: string,
};

export default ConfirmModal;
