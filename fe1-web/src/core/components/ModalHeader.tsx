import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { TouchableOpacity } from 'react-native-gesture-handler';

import { ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    marginBottom: Spacing.x1,
  } as ViewStyle,
  title: {
    ...ModalStyles.modalTitle,
  } as TextStyle,
  done: {
    position: 'absolute',
    right: 0,
  } as TextStyle,
});

const ModalHeader = (props: IPropTypes) => {
  const { children: title, onClose } = props;

  return (
    <View style={styles.header}>
      <Text style={styles.title}>{title}</Text>
      <TouchableOpacity containerStyle={styles.done} onPress={onClose} testID="modal-header-close">
        <Text style={Typography.pressable}>{STRINGS.general_done}</Text>
      </TouchableOpacity>
    </View>
  );
};

const propTypes = {
  children: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
ModalHeader.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ModalHeader;
