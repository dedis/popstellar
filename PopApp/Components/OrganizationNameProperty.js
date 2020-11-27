import React from 'react';
import {
  StyleSheet, View, Text,
} from 'react-native';

import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
 * Organization name property component
 */
const styles = StyleSheet.create({
  view: {
    marginHorizontal: Spacing.s,
    borderWidth: 1,
    borderRadius: 5,
    paddingHorizontal: Spacing.xs,
    marginBottom: Spacing.xs,
  },
  text: {
  },
});

const OrganizationNameProperty = ({ event }) => (
  <View style={styles.view}>
    <Text style={styles.text}>{event.name}</Text>
  </View>
);

OrganizationNameProperty.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default OrganizationNameProperty;
