import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Modal, Text, View } from 'react-native';

import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import modalStyles from '../styles/stylesheets/modalStyles';
import Button from './Button';
import TextInputLine from './TextInputLine';

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
    buttonCancelText,
    onConfirmPress,
    hasTextInput,
    textInputPlaceholder,
  } = props;

  const [textInput, setTextInput] = useState('');

  return (
    <Modal visible={visibility} transparent>
      <View style={modalStyles.modalView}>
        <View style={modalStyles.titleView}>
          <Text style={modalStyles.modalTitle}>{title}</Text>
        </View>
        <Text style={modalStyles.modalDescription}>{description}</Text>
        {hasTextInput ? (
          <TextInputLine
            onChangeText={(input) => setTextInput(input)}
            placeholder={textInputPlaceholder}
          />
        ) : null}
        <View style={modalStyles.buttonView}>
          <Button onPress={() => onConfirmPress(textInput)}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {buttonConfirmText}
            </Text>
          </Button>

          <Button
            onPress={() => {
              setVisibility(!visibility);
              setTextInput('');
            }}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {buttonCancelText}
            </Text>
          </Button>
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
  visibility: boolean;
  setVisibility: Function;
  title: string;
  description: string;
  buttonCancelText: string;
  buttonConfirmText: string;
  onConfirmPress: Function;
  hasTextInput: boolean;
  textInputPlaceholder: string;
};

export default ConfirmModal;
