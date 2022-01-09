import React, { useEffect, useState } from 'react';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';
import { Spacing } from 'styles';
import styleContainer from 'styles/stylesheets/container';

import QrReader from 'react-qr-reader';
import STRINGS from 'res/strings';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import ConfirmModal from 'components/ConfirmModal';
import { Badge } from 'react-native-elements';
import { useNavigation } from '@react-navigation/native';
import { useRoute } from '@react-navigation/core';
import { requestCloseRollCall } from 'network';
import {
  EventTags, Hash, PublicKey, Wallet,
} from 'model/objects';
import { makeCurrentLao, OpenedLaoStore } from 'store';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

/**
 * UI for a currently opened roll call. From there, the organizer can scan attendees or add them
 * manually. At the end, he can close it by pressing on the close button.
 */

const styles = StyleSheet.create({
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

const FOUR_SECONDS = 4000;
const base64Matcher = new RegExp('^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})([=]{1,2})?$');

const RollCallOpened = () => {
  const route = useRoute();
  const { rollCallID, time } = route.params;
  const navigation = useNavigation();
  const [attendees, updateAttendees] = useState(new Set<string>());
  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);
  const toast = useToast();
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  if (!lao) {
    throw new Error('Impossible to open a Roll Call without being connected to an LAO');
  }

  // This will run only when the state changes
  useEffect(() => {
    // Add the token of the organizer as soon as we open the roll call
    Wallet.generateToken(lao.id, new Hash(rollCallID)).then((token) => {
      updateAttendees((prev) => new Set<string>(prev.add(token.publicKey.valueOf())));
    });
  }, []);

  const handleError = (err: string) => {
    console.error(err);
  };

  const handleScan = (data: string | null) => {
    if (data) {
      if (!attendees.has(data)) {
        updateAttendees((prev) => new Set<string>(prev.add(data)));
        toast.show(STRINGS.roll_call_scan_participant, {
          type: 'success',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      }
    }
  };

  const handleEnterManually = (input: string) => {
    if (base64Matcher.test(input)) {
      if (!attendees.has(input)) {
        updateAttendees((prev) => new Set<string>(prev.add(input)));
        toast.show(STRINGS.roll_call_participant_added, {
          type: 'success',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      }
    } else {
      toast.show(STRINGS.roll_call_invalid_token, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  const onCloseRollCall = () => {
    const updateId = Hash.fromStringArray(
      EventTags.ROLL_CALL, OpenedLaoStore.get().id.toString(),
      rollCallID, time,
    );
    const attendeesList = Array.from(attendees).map((key: string) => new PublicKey(key));

    return requestCloseRollCall(updateId, attendeesList).then(() => {
      // @ts-ignore
      navigation.navigate(STRINGS.organizer_navigation_tab_home);
    }).catch((err) => {
      toast.show(`Could not close roll call, error: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    });
  };

  return (
    <View style={styleContainer.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.roll_call_scan_description} />
        <QrReader
          delay={300}
          onScan={(data) => handleScan(data)}
          onError={handleError}
          style={{ width: '30%' }}
        />
        <Badge value={attendees.size} status="success" />
        <WideButtonView
          title={STRINGS.roll_call_scan_close}
          onPress={() => onCloseRollCall()}
        />
        <WideButtonView
          title={STRINGS.roll_call_add_attendee_manually}
          onPress={() => setInputModalIsVisible(true)}
        />
      </View>
      <ConfirmModal
        visibility={inputModalIsVisible}
        setVisibility={setInputModalIsVisible}
        title={STRINGS.roll_call_modal_add_attendee}
        description={STRINGS.roll_call_modal_enter_token}
        onConfirmPress={handleEnterManually}
        buttonConfirmText={STRINGS.general_add}
        hasTextInput
      />
    </View>
  );
};

export default RollCallOpened;
