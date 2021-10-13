import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle, TextStyle,
} from 'react-native';

import { Spacing, Typography } from 'styles';
import styleContainer from 'styles/stylesheets/container';

import QrReader from 'react-qr-reader';
import STRINGS from 'res/strings';
import PropTypes from 'prop-types';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { Badge } from "react-native-elements";
import PROPS_TYPE from '../../../res/Props';

const styles = StyleSheet.create({
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

const RollCall = ({ navigation }: IPropTypes) => {
  const [QrWasScanned, setQrWasScanned] = useState(false);
  const [hasScanned, setHasScanned] = useState(0);

  const handleError = (err: string) => {
    console.error(err);
  };

  const handleScan = (data: string) => {
    if (data) {
      setQrWasScanned(true);
      setHasScanned((prev) => prev + 1);
    }
  };
  return (
    <View style={styleContainer.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.roll_call_scan_description} />
        <QrReader
          delay={300}
          onScan={handleScan}
          onError={handleError}
          style={{ width: '30%' }}
        />
        <Badge value={hasScanned} status="success" />
        <WideButtonView
          title={STRINGS.roll_call_scan_close}
          onPress={() => {
            navigation.navigate(STRINGS.roll_call_scan_close_confirmation);
          }}
        />
      </View>
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
RollCall.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCall;
