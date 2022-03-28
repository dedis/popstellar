import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

const styles = StyleSheet.create({
  view: {
    borderTopColor: '#000',
    borderTopWidth: 1,
  } as ViewStyle,
});

const ListSeparator = () => <View style={styles.view} />;

export default ListSeparator;
