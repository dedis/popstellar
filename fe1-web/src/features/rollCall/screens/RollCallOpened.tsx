import { useNavigation, useRoute } from '@react-navigation/core';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { Badge } from 'react-native-elements';
import { useToast } from 'react-native-toast-notifications';
import QrReader from 'react-qr-reader';
import { useSelector } from 'react-redux';

import { ConfirmModal, TextBlock, WideButtonView } from 'core/components';
import { PublicKey } from 'core/objects';
import { Spacing } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { makeEventGetter } from 'features/events/reducer';
import { selectCurrentLao } from 'features/lao/reducer';
import * as Wallet from 'features/wallet/objects';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { requestCloseRollCall } from '../network';
import { RollCall } from '../objects';

/**
 * UI for a currently opened roll call. From there, the organizer can scan attendees or add them
 * manually. At the end, he can close it by pressing on the close button.
 */

const styles = StyleSheet.create({
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

const tokenMatcher = new RegExp('^[A-Za-z0-9_-]{43}=$');

const RollCallOpened = () => {
  // FIXME: navigation and route should user proper type
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { rollCallID } = route.params;
  const [attendees, updateAttendees] = useState(new Set<string>());
  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);
  const toast = useToast();
  const lao = useSelector(selectCurrentLao);

  if (!lao) {
    throw new Error('Impossible to open a Roll Call without being connected to an LAO');
  }

  const rollCall = useSelector(makeEventGetter(lao.id, rollCallID)) as RollCall;

  if (!rollCall) {
    throw new Error('Impossible to open a Roll Call that does not exist');
  }

  // This will run only when the state changes
  useEffect(() => {
    if (!lao || !lao.id || !toast) {
      return;
    }

    const addOwnToken = async () => {
      try {
        const tok = await Wallet.generateToken(lao.id, rollCall.id);
        updateAttendees((prev) => new Set<string>([...prev, tok.publicKey.valueOf()]));
      } catch (err) {
        toast.show(`Could not generate organizer's PoP token, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      }
    };

    // Add the token of the organizer as soon as we open the roll call
    if (!rollCall.attendees) {
      addOwnToken().catch((e) => console.error(e));
    }
  }, [lao, rollCall, toast]);

  const handleError = (err: any) => {
    console.error(err.toString());
    toast.show(err.toString(), {
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
    const screenAttendeesList = Array.from(attendees).map((key: string) => new PublicKey(key));
    const attendeesList = rollCall.attendees
      ? rollCall.attendees.concat(screenAttendeesList)
      : screenAttendeesList;
    if (!rollCall.idAlias) {
      throw new Error('Trying to close a roll call that has no idAlias defined');
    }
    return requestCloseRollCall(rollCall.idAlias, attendeesList)
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
