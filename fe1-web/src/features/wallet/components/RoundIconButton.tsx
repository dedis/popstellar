import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, TouchableOpacity, ViewStyle } from 'react-native';
import { Icon } from 'react-native-elements';

const styles = StyleSheet.create({
  roundButton: {
    height: 80,
    width: 80,
    shadowColor: 'gray',
    shadowOffset: {
      width: 2,
      height: 2,
    },
    borderRadius: 30,
    backgroundColor: '#2196F3',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
  } as ViewStyle,
});

const RoundIconButton = (props: IPropTypes) => {
  const { name, onClick } = props;
  return (
    <TouchableOpacity style={styles.roundButton} onPress={onClick}>
      <Icon name={name} type="material" color="white" size={30} />
    </TouchableOpacity>
  );
};

const propTypes = {
  name: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
};
RoundIconButton.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RoundIconButton;
