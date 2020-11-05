import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';
import { connect } from 'react-redux';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';

/**
* The LAO item component
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

const handlePress = (navigation, LAO, dispatch) => {
  const action = { type: 'APP_NAVIGATION_ON', value: LAO.id };
  dispatch(action);
  navigation.navigate(STRINGS.app_navigation_tab_organizer);
};

const LAOItem = ({ LAO, dispatch }) => {
  const navigation = useNavigation();

  return (
    <View style={styles.view}>
      <TouchableOpacity onPress={() => handlePress(navigation, LAO, dispatch)}>
        <Text style={styles.text}>{LAO.name}</Text>
      </TouchableOpacity>
    </View>
  );
};

LAOItem.propTypes = {
  LAO: PropTypes.shape({
    name: PropTypes.string.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = (state) => (
  {
    organizationNavigation: state.organizationNavigation,
  }
);

export default connect(mapStateToProps)(LAOItem);
