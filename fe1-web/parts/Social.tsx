import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import styleContainer from 'styles/stylesheets/container';

import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';
import WideButtonView from 'components/WideButtonView';

/**
 * UI for the Social Media component
 */
const styles = StyleSheet.create({
  viewTop: {
    justifyContent: 'flex-start',
  } as ViewStyle,
  viewBottom: {
    justifyContent: 'flex-end',
  } as ViewStyle,
});

const sendMessage = (message: String) => message;

const Social = () => (
  <View style={styleContainer.flex}>
    <View style={styles.viewTop}>
      <TextBlock text={STRINGS.feed_description} />
    </View>
    <View style={styles.viewBottom}>
      <WideButtonView
        title={STRINGS.button_post_test_message}
        onPress={sendMessage}
      />
    </View>
  </View>
);

export default Social;
