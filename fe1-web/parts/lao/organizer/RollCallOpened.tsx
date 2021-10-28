import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';
import { Spacing } from 'styles';
import styleContainer from 'styles/stylesheets/container';

import QrReader from 'react-qr-reader';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { Badge } from 'react-native-elements';
import { useNavigation } from '@react-navigation/native';
import { useRoute } from '@react-navigation/core';
import { requestCloseRollCall } from 'network';
import { EventTags, Hash, PublicKey } from 'model/objects';
import { OpenedLaoStore } from 'store';

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
  const { rollCallID, time } = route.params;
  const navigation = useNavigation();
  const [, setQrWasScanned] = useState(false);
  const [attendeesSet, updateAttendeesSet] = useState(new Set<string>());
  const attendees = Array.from(attendeesSet);

  const handleError = (err: string) => {
    console.error(err);
  };

  const handleScan = (data: string) => {
    if (data) {
      setQrWasScanned(true);
      updateAttendeesSet((prev) => new Set<string>(prev.add(data)));
      // TODO: use toast to display the scanned message, instead of console.log
      console.log(STRINGS.roll_call_scan_participant);
    }
  };

  const onCloseRollCall = () => {
    const updateId = Hash.fromStringArray(
      EventTags.ROLL_CALL, OpenedLaoStore.get().id.toString(),
      rollCallID, time,
    );
    requestCloseRollCall(updateId, attendees.map((key: string) => new PublicKey(key)))
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
        <Badge value={attendeesSet.size} status="success" />
        <WideButtonView
          title={STRINGS.roll_call_scan_close}
          onPress={() => onCloseRollCall()}
        />
      </View>
    </View>
  );
};

export default RollCallOpened;
