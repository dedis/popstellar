import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';

import { Color, Icon, ModalStyles, Spacing } from 'core/styles';

import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: Spacing.x1,
  } as ViewStyle,
  title: {
    ...ModalStyles.modalTitle,
  } as TextStyle,
});

const ModalHeader = (props: IPropTypes) => {
  const { children: title, onClose } = props;

  return (
    <View style={styles.header}>
      <View />
      <Text style={styles.title} numberOfLines={1}>
        {title}
      </Text>
      <PoPTouchableOpacity onPress={onClose} testID="modal-header-close">
        <PoPIcon name="close" color={Color.primary} size={Icon.size} />
      </PoPTouchableOpacity>
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
