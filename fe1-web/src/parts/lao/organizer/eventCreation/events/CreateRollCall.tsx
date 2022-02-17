import React, { useState } from 'react';
import {
  View, Platform, ScrollView,
} from 'react-native';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';

import STRINGS from 'res/strings';
import { requestCreateRollCall } from 'network/MessageApi';
import DatePicker, { onChangeStartTime, onChangeEndTime } from 'components/DatePicker';
import ParagraphBlock from 'components/ParagraphBlock';
import WideButtonView from 'components/WideButtonView';
import TextInputLine from 'components/TextInputLine';
import DismissModal from 'components/DismissModal';
import ConfirmModal from 'components/ConfirmModal';
import { Timestamp } from 'model/objects';
import { useToast } from 'react-native-toast-notifications';
import { FOUR_SECONDS } from 'res/const';
import { onConfirmPress } from '../CreateEvent';

const DEFAULT_ROLL_CALL_DURATION = 3600;

/**
 * Screen to create a roll-call event
 *
 * TODO Send the Roll-call event in an open state to the organization server
 *  when the confirm button is press
 */
const CreateRollCall = ({ route }: any) => {
  const styles = route.params;
  const navigation = useNavigation();
  const toast = useToast();

  const [proposedStartTime, setProposedStartTime] = useState(Timestamp.EpochNow());
  const [proposedEndTime, setProposedEndTime] = useState(Timestamp.EpochNow()
    .addSeconds(DEFAULT_ROLL_CALL_DURATION));

  const [rollCallName, setRollCallName] = useState('');
  const [rollCallLocation, setRollCallLocation] = useState('');
  const [rollCallDescription, setRollCallDescription] = useState('');
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);

  const buildDatePickerWeb = () => {
    const startDate = proposedStartTime.timestampToDate();
    const endDate = proposedEndTime.timestampToDate();

    return (
      <View style={styles.viewVertical}>
        <View style={[styles.view, { padding: 5 }]}>
          <ParagraphBlock text={STRINGS.roll_call_create_proposed_start} />
          <DatePicker
            selected={startDate}
            onChange={(date: Date) => onChangeStartTime(date, setProposedStartTime,
              setProposedEndTime, DEFAULT_ROLL_CALL_DURATION)}
          />
        </View>
        <View style={[styles.view, { padding: 5, zIndex: 'initial' }]}>
          <ParagraphBlock text={STRINGS.roll_call_create_proposed_end} />
          <DatePicker
            selected={endDate}
            onChange={(date: Date) => onChangeEndTime(date, proposedStartTime, setProposedEndTime)}
          />
        </View>
      </View>
    );
  };

  const buttonsVisibility: boolean = (rollCallName !== '' && rollCallLocation !== '');

  const createRollCall = () => {
    const description = (rollCallDescription === '') ? undefined : rollCallDescription;
    requestCreateRollCall(
      rollCallName, rollCallLocation, proposedStartTime, proposedEndTime,
      description,
    )
      .then(() => {
        navigation.navigate(STRINGS.organizer_navigation_tab_home);
      })
      .catch((err) => {
        console.error('Could not create roll call, error:', err);
        toast.show(`Could not create roll call, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  return (
    <ScrollView>
      { /* see archive branches for date picker used for native apps */ }
      { Platform.OS === 'web' && buildDatePickerWeb() }

      <TextInputLine
        placeholder={STRINGS.roll_call_create_name}
        onChangeText={(text: string) => { setRollCallName(text); }}
      />
      <TextInputLine
        placeholder={STRINGS.roll_call_create_location}
        onChangeText={(text: string) => { setRollCallLocation(text); }}
      />
      <TextInputLine
        placeholder={STRINGS.roll_call_create_description}
        onChangeText={(text: string) => { setRollCallDescription(text); }}
      />

      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={() => onConfirmPress(proposedStartTime, proposedEndTime, createRollCall,
          setModalStartIsVisible, setModalEndIsVisible)}
        disabled={!buttonsVisibility}
      />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={navigation.goBack}
      />

      <DismissModal
        visibility={modalEndIsVisible}
        setVisibility={setModalEndIsVisible}
        title={STRINGS.modal_event_creation_failed}
        description={STRINGS.modal_event_ends_in_past}
      />
      <ConfirmModal
        visibility={modalStartIsVisible}
        setVisibility={setModalStartIsVisible}
        title={STRINGS.modal_event_creation_failed}
        description={STRINGS.modal_event_starts_in_past}
        onConfirmPress={() => createRollCall()}
        buttonConfirmText={STRINGS.modal_button_start_now}
        buttonCancelText={STRINGS.modal_button_go_back}
      />
    </ScrollView>
  );
};

export default CreateRollCall;
