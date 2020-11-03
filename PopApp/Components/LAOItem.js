import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';

/*
* The LAO item component
*
*
*/
const styles = StyleSheet.create({
  view: {
    marginBottom: Spacing.xs,
  },
  text: {
    ...Typography.base,
    borderWidth: 1,
    borderRadius: 5,
  },
});

const handlePress = (navigation, LAO, props) => {
  const parentNavigation = navigation.dangerouslyGetParent();
  console.log(parentNavigation);
  console.log(navigation);
  console.log(props);
  if (parentNavigation !== undefined) {
    // parentNavigation.reset(STRINGS.app_navigation_tab_organizer);
    parentNavigation.navigate(STRINGS.app_navigation_tab_organizer);
  }
};

const LAOItem = (props) => {
  const navigation = useNavigation();
  const { LAO } = props;

  return (
    <View style={styles.view}>
      <TouchableOpacity onPress={() => handlePress(navigation, LAO, props)}>
        <Text style={styles.text}>{LAO.name}</Text>
      </TouchableOpacity>
    </View>
  );
};

LAOItem.propTypes = {
  LAO: PropTypes.shape({
    name: PropTypes.string.isRequired,
  }).isRequired,
};

export default LAOItem;
