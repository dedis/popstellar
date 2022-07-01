import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { ConfirmModal, PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Icon } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
import { makeRollCallSelector } from '../reducer';

/**
 * UI for a currently opened roll call. From there, the organizer can scan attendees or add them
 * manually. At the end, he can close it by pressing on the close button.
 */

const styles = StyleSheet.create({
  buttonContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  leftButtons: QrCodeScannerUIElementContainer,
  rightButtons: QrCodeScannerUIElementContainer,
});

const tokenMatcher = new RegExp('^[A-Za-z0-9_-]{43}=$');

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_open_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const RollCallOpened = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();
  const { rollCallId, initialAttendeePopTokens } = route.params;
  const [attendeePopTokens, updateAttendeePopTokens] = useState(
    new Set<string>(initialAttendeePopTokens),
  );
  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);
  const toast = useToast();

  const laoId = RollCallHooks.useCurrentLaoId();
  const generateToken = RollCallHooks.useGenerateToken();

  const rollCallSelector = useMemo(() => makeRollCallSelector(rollCallId), [rollCallId]);
  const rollCall = useSelector(rollCallSelector);

  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);

  // re-enable scanner on focus events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      // The screen is now focused, set showScanner to true
      setShowScanner(true);
    });
  }, [navigation]);
  // disable scanner on blur events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('blur', () => {
      // The screen is no longer focused, set showScanner to false (i.e. allow scanner to be reused)
      setShowScanner(false);
    });
  }, [navigation]);

  if (!laoId) {
    throw new Error('Impossible to open a Roll Call without being connected to an LAO');
  }

  if (!rollCall) {
    throw new Error('Impossible to open a Roll Call that does not exist');
  }

  const handleError = useCallback(
    (err: string | Error) => {
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

  const addAttendeePopTokenAndShowToast = (popToken: string) => {
    if (tokenMatcher.test(popToken)) {
      // only show a toast if an actual *new* token is added
      if (addAttendeePopToken(popToken)) {
        toast.show(STRINGS.roll_call_scan_participant, {
          type: 'success',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      } else {
        toast.show(STRINGS.roll_call_scan_participant_twice, {
          type: 'danger',
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
    <>
      <QrCodeScanner
        showCamera={showScanner}
        handleScan={(data) => {
          if (data) {
            addAttendeePopTokenAndShowToast(data);
          }
        }}>
        <View style={styles.buttonContainer}>
          <View>
            <View style={styles.leftButtons}>
              <PoPTouchableOpacity
                testID="roll-call-open-stop-scanning"
                onPress={() =>
                  navigation.navigate(STRINGS.navigation_lao_events_view_single_roll_call, {
                    eventId: rollCallId,
                    /* this screen is only reachable for organizers */
                    isOrganizer: true,
                    /* pass the just scanned pop tokens back to the single view screen */
                    attendeePopTokens: [...attendeePopTokens],
                  })
                }>
                <PoPIcon name="close" color={Color.accent} size={Icon.size} />
              </PoPTouchableOpacity>
            </View>
          </View>
          <View>
            <View style={styles.rightButtons}>
              <PoPTouchableOpacity
                onPress={() => setInputModalIsVisible(true)}
                testID="roll-call-open-add-manually">
                <PoPIcon name="addPerson" color={Color.accent} size={Icon.size} />
              </PoPTouchableOpacity>
            </View>
          </View>
        </View>
      </QrCodeScanner>
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
    </>
  );
};

export default RollCallOpened;

export const RollCallOpenedScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_open_roll_call,
  Component: RollCallOpened,
  headerShown: false,
};
