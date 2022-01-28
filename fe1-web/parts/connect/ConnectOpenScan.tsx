import React, { useState } from 'react';
import { ActivityIndicator, View } from 'react-native';

// @ts-ignore
import QrReader from 'react-qr-reader';
import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import PropTypes from 'prop-types';
import styleContainer from 'styles/stylesheets/container';
import { Colors } from 'styles';
import WideButtonView from 'components/WideButtonView';
import { useToast } from 'react-native-toast-notifications';

const FOUR_SECONDS = 4000;

/**
 * Starts a QR code scan
 *
 * TODO use the camera to scan a QR code and give the URL find to the ConnectConnecting component
 */
const ConnectOpenScan = ({ navigation }: IPropTypes) => {
  // Remove the user to go back to the ConnectEnableCamera as he has already given
  // his permission to use the camera
  const [QrWasScanned, setQrWasScanned] = useState(false);
  const toast = useToast();

  const handleError = (err: string) => {
    console.error(err);
    toast.show(err, {
      type: 'danger',
      placement: 'top',
      duration: FOUR_SECONDS,
    });
  };

  const handleScan = (data: string) => {
    console.log(data);
    if (data) {
      setQrWasScanned(true);
      navigation.navigate(STRINGS.connect_confirm_title, { laoIdIn: data });
    }
  };

  return QrWasScanned
    ? (
      <View style={styleContainer.centered} />
    ) : (
      <View style={styleContainer.centered}>
        <QrReader
          delay={300}
          onError={handleError}
          onScan={handleScan}
          style={{ width: '30%' }}
        />
        <ActivityIndicator size="large" color={Colors.blue} />
        <WideButtonView
          title={STRINGS.general_button_cancel}
          onPress={() => {
            setQrWasScanned(true);
            navigation.navigate(STRINGS.connect_unapproved_title);
          }}
        />
      </View>
    );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
ConnectOpenScan.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ConnectOpenScan;
