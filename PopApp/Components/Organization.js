import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import STRINGS from '../res/strings';
import { Views } from '../Styles';

/**
* The Organization component
*
* Manage the Organization screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    ...Views.base,
  },
});

const Organization = () => (
  <View style={styles.container}>
    <Text>{STRINGS.organization_description}</Text>
  </View>
);

export default Organization;
