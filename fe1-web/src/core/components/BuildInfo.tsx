import Constants from 'expo-constants';
import React from 'react';
import { StyleSheet, View, Linking, Text, ViewStyle } from 'react-native';

import { Spacing, Typography } from 'core/styles';

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: Spacing.x05,
    left: Spacing.x05,
    zIndex: 100,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
});

const BuildInfo = () => {
  return (
    <View style={styles.container}>
      <Text
        style={[Typography.minuscule, Typography.inactive, Typography.code, Typography.uppercase]}>
        <Text
          onPress={() =>
            Linking.openURL(
              `https://github.com/dedis/popstellar/releases/tag/${Constants?.expoConfig?.extra?.appVersion}`,
            )
          }>
          {Constants?.expoConfig?.extra?.appVersion}
        </Text>
        <Text> | </Text>
        <Text onPress={() => Linking.openURL(Constants?.expoConfig?.extra?.buildURL)}>
          {Constants?.expoConfig?.extra?.shortSHA}
        </Text>
        <Text> | </Text>
        <Text>{Constants?.expoConfig?.extra?.buildDate}</Text>
      </Text>
    </View>
  );
};

export default BuildInfo;
