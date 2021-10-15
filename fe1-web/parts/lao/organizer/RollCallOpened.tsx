import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';
import { Spacing } from 'styles';
import styleContainer from 'styles/stylesheets/container';

import QrReader from 'react-qr-reader';
import STRINGS from 'res/strings';
import PropTypes from 'prop-types';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { Badge } from 'react-native-elements';
import { useNavigation } from '@react-navigation/native';
import { useRoute } from '@react-navigation/core';
import { requestCloseRollCall } from '../../../network';
import { PublicKey, RollCall } from '../../../model/objects';

const styles = StyleSheet.create({
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

const RollCallOpened = () => {
  const route = useRoute();
  const { rollCall } = route.params;
  const navigation = useNavigation();
  const [, setQrWasScanned] = useState(false);
  const [attendees, updateAttendees] = useState<string[]>([]);

  const handleError = (err: string) => {
    console.error(err);
  };

  const handleScan = (data: string) => {
    if (data) {
      setQrWasScanned(true);
      updateAttendees((arr) => [...arr, data]);
    }
  };

  // Here we get the pop-token to display in the QR code
  // @ts-ignore
  /* Wallet.generateToken(lao.id, rollCall.id)
    .then((token) => setPopToken(token.publicKey.valueOf())); */

  const onCloseRollCall = () => {
    requestCloseRollCall(rollCall.id, attendees.map((key: string) => new PublicKey(key)))
      .then(() => {
        // @ts-ignore
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not close roll call, error: ', err);
      });
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
        <Badge value={attendees.length} status="success" />
        <WideButtonView
          title={STRINGS.roll_call_scan_close}
          onPress={() => onCloseRollCall()}
        />
        {/* {!isOrganizer && (
          <>
            <Text>Let the organizer scan your Pop token</Text>
            <QRCode visibility value={popToken} />
          </>
        )} */}
      </View>
    </View>
  );
};

/* const propTypes = {
  rollCall: PropTypes.instanceOf(RollCall).isRequired,
};
RollCallOpened.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>; */

export default RollCallOpened;
