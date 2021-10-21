import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import styleContainer from 'styles/stylesheets/container';

import TextBlock from 'components/TextBlock';
import STRINGS from 'res/strings';
import WideButtonView from 'components/WideButtonView';

import { requestAddChirp } from 'network/MessageApi';

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

const publishChirp = () => {
  requestAddChirp('chirp chirp')
    .then()
    .catch((err) => {
      console.error('Could not add chirp, error:', err);
    });
};

const Social = () => (
  <View style={styleContainer.flex}>
    <View style={styles.viewTop}>
      <TextBlock text={STRINGS.feed_description} />
    </View>
    <View style={styles.viewBottom}>
      <WideButtonView
        title={STRINGS.button_publish_test_chirp}
        onPress={publishChirp}
      />
    </View>
  </View>
);

export default Social;
