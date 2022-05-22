import { useNavigation, useRoute } from '@react-navigation/core';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { Badge } from 'react-native-elements';
import { useToast } from 'react-native-toast-notifications';
import QrReader from 'react-qr-reader';
import { useSelector } from 'react-redux';

import { ConfirmModal, TextBlock, WideButtonView } from 'core/components';
import { PublicKey } from 'core/objects';
import { Spacing } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { requestCloseRollCall } from '../network';
import { makeRollCallSelector } from '../reducer';

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

const qrScannerStyles: ViewStyle = {
  width: '30%',
};

const tokenMatcher = new RegExp('^[A-Za-z0-9_-]{43}=$');

const RollCallOpened = () => {
  // FIXME: navigation and route should user proper type
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { rollCallID } = route.params;
  const [attendeePopTokens, updateAttendeePopTokens] = useState(new Set<string>());
  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);
  const toast = useToast();

  const laoId = RollCallHooks.useCurrentLaoId();
  const generateToken = RollCallHooks.useGenerateToken();

  const rollCallSelector = useMemo(() => makeRollCallSelector(rollCallID), [rollCallID]);
  const rollCall = useSelector(rollCallSelector);

  if (!laoId) {
    throw new Error('Impossible to open a Roll Call without being connected to an LAO');
  }

  if (!rollCall) {
    throw new Error('Impossible to open a Roll Call that does not exist');
  }

  const handleError = useCallback(
    (err: any) => {
      console.error(err.toString());
      toast.show(err.toString(), {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    },
    [toast],
  );

  const addAttendeePopToken = useCallback(
    (popToken: string) => {
      // if the token is already part of attendeePopTokens, do not trigger a state update
      if (attendeePopTokens.has(popToken)) {
        return false;
      }

      updateAttendeePopTokens(
        // use new Set() to trigger a state change. .add() would not still be the same object
        (prevAttendeePopTokens) => new Set<string>([...prevAttendeePopTokens, popToken]),
      );
      return true;
    },
    [updateAttendeePopTokens, attendeePopTokens],
  );

  const addAttendeePopTokenAndShowToast = (popToken: string, toastMessage: string) => {
    if (tokenMatcher.test(popToken)) {
      // only show a toast if an actual *new* token is added
      if (addAttendeePopToken(popToken)) {
        toast.show(toastMessage, {
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

  const onCloseRollCall = async () => {
    // get the public key as strings from the existing rollcall
    const previousAttendees = (rollCall.attendees || []).map((key) => key.valueOf());
    // add the create a set of all attendees (takes care of deduplication)
    const allAttendees = new Set([...previousAttendees, ...attendeePopTokens]);
    // create PublicKey instances from the set of strings
    const attendeesList = [...allAttendees].map((key: string) => new PublicKey(key));

    if (!rollCall.idAlias) {
      throw new Error('Trying to close a roll call that has no idAlias defined');
    }

    try {
      await requestCloseRollCall(laoId, rollCall.idAlias, attendeesList);
      navigation.navigate(STRINGS.organizer_navigation_tab_home);
    } catch (err) {
      toast.show(`Could not close roll call, error: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  // This will run only when the state changes
  useEffect(() => {
    if (!laoId) {
      return;
    }

    // Add the token of the organizer as soon as we open the roll call
    generateToken(laoId, rollCall.id)
      .then((popToken) => addAttendeePopToken(popToken.publicKey.valueOf()))
      .catch(handleError);
  }, [laoId, generateToken, rollCall, addAttendeePopToken, handleError]);

  return (
    <View style={containerStyles.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.roll_call_scan_description} />
        <QrReader
          delay={300}
          onScan={(data) => {
            if (data) {
              addAttendeePopTokenAndShowToast(data, STRINGS.roll_call_scan_participant);
            }
          }}
          onError={handleError}
          style={qrScannerStyles}
        />
        <Badge value={attendeePopTokens.size} status="success" />
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
        onConfirmPress={addAttendeePopTokenAndShowToast}
        buttonConfirmText={STRINGS.general_add}
        hasTextInput
        textInputPlaceholder={STRINGS.roll_call_attendee_token_placeholder}
      />
    </View>
  );
};

export default RollCallOpened;
