import PropTypes from 'prop-types';
import React from 'react';
import { TouchableOpacity } from 'react-native';
import { Icon } from 'react-native-elements';

import circularButtonStyles from '../styles/stylesheets/circularButtonStyles';

/**
 * This is a round button with an icon that uses Google material icons
 * The 'name' property is the name of the icon that will be rendered,
 * you can look for icon and their names here : https://fonts.google.com/icons
 */
const RoundIconButton = (props: IPropTypes) => {
  const { name, onClick } = props;
  return (
    <TouchableOpacity style={circularButtonStyles.roundButton} onPress={onClick}>
      <Icon name={name} type="material" color="white" size={25} />
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
