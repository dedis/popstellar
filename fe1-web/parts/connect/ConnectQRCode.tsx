import React from 'react';
import { View, ActivityIndicator } from 'react-native';
import { Colors } from 'styles/index';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';
import WideButtonView from 'components/WideButtonView';
import TextBlock from 'components/TextBlock';
import styleContainer from 'styles/stylesheets/container';

/**
 * Note : Currently, just simulate waiting for a response
 *
 * TODO make the screen to perform a request to the organizer
 *  server to verify if the user can connect and go to the ConnectConfirm with the information
 *  receive by the server
 */
const ConnectConnecting = ({ navigation }: IPropTypes) => (
  <View style={styleContainer.flex}>
    <TextBlock text={STRINGS.connect_connecting_uri} />
    <ActivityIndicator size="large" color={Colors.blue} />
    <View>
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={() => { navigation.navigate(STRINGS.connect_scanning_title); }}
      />
      <WideButtonView
        title={STRINGS.connect_connecting_validate}
        onPress={() => { navigation.navigate(STRINGS.connect_confirm_title); }}
      />
    </View>
  </View>
);

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
ConnectConnecting.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ConnectConnecting;
