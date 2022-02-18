import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import QrReader from 'react-qr-reader';
import { Badge } from 'react-native-elements';
import { useNavigation } from '@react-navigation/native';
import { useRoute } from '@react-navigation/core';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { Spacing } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';
import { ConfirmModal, TextBlock, WideButtonView } from 'core/components';
import { EventTags, Hash, PublicKey } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { makeCurrentLao } from 'features/lao/reducer';
import { FOUR_SECONDS } from 'resources/const';
import * as Wallet from 'features/wallet/objects';

import { requestCloseRollCall } from '../network/RollCallMessageApi';

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

const tokenMatcher = new RegExp('^[A-Za-z0-9_-]{43}=$');

const RollCallOpened = () => {
  const route = useRoute();
  const { rollCallID, time } = route.params;
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();
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
    toast.show(err, {
      type: 'danger',
      placement: 'top',
      duration: FOUR_SECONDS,
    });
  };

  const addAttendeeAndShowToast = (attendee: string, toastMessage: string) => {
    if (!attendees.has(attendee)) {
      updateAttendees((prev) => new Set<string>(prev.add(attendee)));
      toast.show(toastMessage, {
        type: 'success',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  const handleEnterManually = (input: string) => {
    if (tokenMatcher.test(input)) {
      addAttendeeAndShowToast(input, STRINGS.roll_call_participant_added);
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
      EventTags.ROLL_CALL,
      OpenedLaoStore.get().id.toString(),
      rollCallID,
      time,
    );
    const attendeesList = Array.from(attendees).map((key: string) => new PublicKey(key));

    return requestCloseRollCall(updateId, attendeesList)
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        toast.show(`Could not close roll call, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  return (
    <View style={containerStyles.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.roll_call_scan_description} />
        <QrReader
          delay={300}
          onScan={(data) => {
            if (data) {
              addAttendeeAndShowToast(data, STRINGS.roll_call_scan_participant);
            }
          }}
          onError={handleError}
          style={{ width: '30%' }}
        />
        <Badge value={attendees.size} status="success" />
        <WideButtonView title={STRINGS.roll_call_scan_close} onPress={() => onCloseRollCall()} />
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
        textInputPlaceholder={STRINGS.roll_call_attendee_token_placeholder}
      />
    </View>
  );
};

export default RollCallOpened;
