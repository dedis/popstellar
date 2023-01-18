import React from 'react';
import { StyleSheet, View, Linking, Text } from 'react-native';
import Constants from 'expo-constants';
import { Color, Spacing, Typography } from 'core/styles';

const BuildInfo = () => {
  const styles = StyleSheet.create({
    container: {
      position: 'absolute',
      bottom: Spacing.x05,
      left: Spacing.x05,
      zIndex: 100,
      color: Color.inactive,
      fontFamily: 'monospace',
      textTransform: 'uppercase',
      display: 'flex',
      flexDirection: 'row',
      alignItems: 'center',
      ...Typography.minuscule,
    },
    link: {
      textDecorationLine: 'none',
      color: Color.inactive,
      ...Typography.minuscule,
    },
  });

  return (
    <View style={styles.container}>
      <Text
        style={styles.link}
        onPress={() =>
          Linking.openURL(
            `https://github.com/dedis/popstellar/releases/tag/${Constants?.expoConfig?.extra?.appVersion}`,
          )
        }>
        {Constants?.expoConfig?.extra?.appVersion}
      </Text>
      <Text style={styles.link}> | </Text>
      <Text
        style={styles.link}
        onPress={() => Linking.openURL(Constants?.expoConfig?.extra?.buildURL)}>
        {Constants?.expoConfig?.extra?.shortSHA}
      </Text>
      <Text style={styles.link}> | </Text>
      {Constants?.expoConfig?.extra?.buildDate}
    </View>
  );
};

export default BuildInfo;
