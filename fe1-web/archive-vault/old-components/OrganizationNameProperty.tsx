import React from 'react';
import {
  StyleSheet, View, Text,
} from 'react-native';

import { Spacing } from 'styles';
import PROPS_TYPE from 'res/Props';

/**
 * Organization name property component: a name text
 *
 * Show the name of the LAO
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

// @ts-ignore
const OrganizationNameProperty = ({ event }) => (
  <View style={styles.view}>
    <Text style={styles.text}>{event.name}</Text>
  </View>
);

OrganizationNameProperty.propTypes = {
  event: PROPS_TYPE.property.isRequired,
};

export default OrganizationNameProperty;
