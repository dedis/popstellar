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
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

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
  const [attendees, updateAttendees] = useState<string[]>([]);

  const handleError = (err: string) => {
    console.error(err);
  };

  const handleScan = (data: string) => {
    if (data) {
      if (!attendees.includes(data)) {
        updateAttendees((arr) => [...arr, data]);
        toast.success(STRINGS.roll_call_scan_participant, {
          position: 'top-center',
          autoClose: 5000,
          hideProgressBar: false,
          closeOnClick: true,
          pauseOnHover: true,
          draggable: true,
          progress: undefined,
        });
      }
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
      <ToastContainer
        position="top-center"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
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
      </View>
    </View>
  );
};

export default RollCallOpened;
