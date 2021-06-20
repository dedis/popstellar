import React from 'react';
import { View } from 'react-native';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';
import TextBlock from 'components/TextBlock';
import CameraButton from 'components/CameraButton';
import styleContainer from 'styles/stylesheets/container';

/**
 * Starts a QR code scan
 *
 * TODO use the camera to scan a QR code and give the URL find to the ConnectConnecting component
*/
const ConnectOpenScan = ({ navigation }: IPropTypes) => {
  // Remove the user to go back to the ConnectEnableCamera as he has already given
  // his permission to use the camera
  // Note : useEffect = componentDidMount + componentDidUpdate + componentWillUnmount together
  React.useEffect(
    () => navigation.addListener('beforeRemove', (e: any) => {
      e.preventDefault();
    }),
    [navigation],
  );

  return (
    <View style={styleContainer.anchoredCenter}>
      <TextBlock text={STRINGS.connect_scanning_camera_view} />
      <CameraButton action={() => { navigation.navigate(STRINGS.connect_connecting_title); }} />
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
ConnectOpenScan.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ConnectOpenScan;
