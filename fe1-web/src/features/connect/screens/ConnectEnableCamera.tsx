import { useNavigation } from '@react-navigation/core';
import React from 'react';
import { View } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

/**
 * ConnectEnableCamera pane asks for the user to enable camera access in order to scan QR codes
 */
const ConnectEnableCamera = () => {
  // FIXME: route should use proper type
  const navigation = useNavigation<any>();

  return (
    <View style={containerStyles.flex}>
      <TextBlock text={STRINGS.connect_description} />
      <WideButtonView
        title={STRINGS.connect_button_camera}
        onPress={() => {
          navigation.navigate(STRINGS.connect_scanning_title);
        }}
      />
      <WideButtonView
        title={STRINGS.connect_connecting_validate}
        onPress={() => {
          navigation.navigate(STRINGS.connect_confirm_title);
        }}
      />
    </View>
  );
};

export default ConnectEnableCamera;
