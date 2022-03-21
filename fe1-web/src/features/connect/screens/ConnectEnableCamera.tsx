import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

import { TextBlock, WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import PROPS_TYPE from 'resources/Props';
import STRINGS from 'resources/strings';

/**
 * ConnectEnableCamera pane asks for the user to enable camera access in order to scan QR codes
 */
const ConnectEnableCamera = ({ navigation }: IPropTypes) => (
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

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
ConnectEnableCamera.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ConnectEnableCamera;
