import React from 'react';
import { Ionicons } from '@expo/vector-icons';
import PropTypes from 'prop-types';
import { Pressable, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';

/**
 * UI button to go back in navigation. You can specify the string corresponding to where it should
 * navigate along with its size in its properties.
 */
const BackButton = (props: IPropTypes) => {
  const { navigationTabName, size } = props;
  const navigation = useNavigation();

  return (
    // The view is there to set the button's clickable layout correctly.
    <View style={{ width: size }}>
      <Pressable onPress={() => navigation.navigate(navigationTabName as never)}>
        <Ionicons name="chevron-back-outline" size={size} />
      </Pressable>
    </View>
  );
};

const propTypes = {
  navigationTabName: PropTypes.string.isRequired,
  size: PropTypes.number,
};

type IPropTypes = {
  navigationTabName: string,
  size: number,
};

BackButton.defaultProps = {
  size: 23,
};

BackButton.propTypes = propTypes;

export default BackButton;
