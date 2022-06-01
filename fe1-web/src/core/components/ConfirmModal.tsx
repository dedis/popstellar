import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Modal, Text } from 'react-native';
import { ScrollView, TouchableWithoutFeedback } from 'react-native-gesture-handler';

import { ModalStyles, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import Input from './Input';
import ModalHeader from './ModalHeader';
import PoPTextButton from './PoPTextButton';

/**
 * A modal used to ask for the confirmation or cancellation of the user.
 * It can also ask for a user input.
 */

const ConfirmModal = (props: IPropTypes) => {
  const {
    visibility,
    setVisibility,
    title,
    description,
    buttonConfirmText,
    onConfirmPress,
    hasTextInput,
    textInputPlaceholder,
  } = props;

  const [textInput, setTextInput] = useState('');

  return (
    <Modal
      transparent
      visible={visibility}
      onRequestClose={() => {
        setVisibility(!visibility);
      }}>
      <TouchableWithoutFeedback
        containerStyle={ModalStyles.modalBackground}
        onPress={() => {
          setVisibility(!visibility);
        }}
      />
      <ScrollView style={ModalStyles.modalContainer}>
        <ModalHeader onClose={() => setVisibility(!visibility)}>{title}</ModalHeader>
        <Text style={Typography.paragraph}>{description}</Text>
        {hasTextInput ? (
          <Input
            testID="confirm-modal-input"
            value={textInput}
            onChange={setTextInput}
            placeholder={textInputPlaceholder}
            border
          />
        ) : null}
        <PoPTextButton onPress={() => onConfirmPress(textInput)} testID="confirm-modal-confirm">
          {buttonConfirmText}
        </PoPTextButton>
      </ScrollView>
    </Modal>
  );
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
  setVisibility: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  buttonConfirmText: PropTypes.string,
  onConfirmPress: PropTypes.func.isRequired,
  hasTextInput: PropTypes.bool,
  textInputPlaceholder: PropTypes.string,
};

ConfirmModal.propTypes = propTypes;

ConfirmModal.defaultProps = {
  buttonConfirmText: STRINGS.general_button_confirm,
  hasTextInput: false,
  textInputPlaceholder: '',
};

type IPropTypes = {
  visibility: boolean;
  setVisibility: Function;
  title: string;
  description: string;
  buttonConfirmText: string;
  onConfirmPress: Function;
  hasTextInput: boolean;
  textInputPlaceholder: string;
};

export default ConfirmModal;
