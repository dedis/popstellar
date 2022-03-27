import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';
import React from 'react';
import { Pressable, View } from 'react-native';

/**
 * UI button to go back in navigation. You can specify the string corresponding to where it should
 * navigate along with its size in its properties.
 */
const BackButton = (props: IPropTypes) => {
  const { navigationTabName, size, testID } = props;
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  return (
    // The view is there to set the button's clickable layout correctly.
    <View style={{ width: size }}>
      <Pressable onPress={() => navigation.navigate(navigationTabName)} testID={testID}>
        <Ionicons name="chevron-back-outline" size={size} />
      </Pressable>
    </View>
  );
};

const propTypes = {
  navigationTabName: PropTypes.string.isRequired,
  size: PropTypes.number,
  testID: PropTypes.string,
};

type IPropTypes = {
  navigationTabName: string;
  size: number;
  testID: string;
};

BackButton.defaultProps = {
  size: 23,
  testID: 'backButton',
};

BackButton.propTypes = propTypes;

export default BackButton;
