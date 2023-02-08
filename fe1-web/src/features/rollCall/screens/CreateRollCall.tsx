import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Platform, Text } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { ConfirmModal, DatePicker, DismissModal, Input } from 'core/components';
import { onChangeEndTime, onChangeStartTime } from 'core/components/DatePicker';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { onConfirmEventCreation } from 'core/functions/UI';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Timestamp } from 'core/objects';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { RollCallHooks } from '../hooks';
import { RollCallFeature } from '../interface';
import { requestCreateRollCall } from '../network';

const DEFAULT_ROLL_CALL_DURATION = 3600;

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.events_create_roll_call>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * Screen to create a roll-call event
 *
 * TODO Send the Roll-call event in an open state to the organization server
 *  when the confirm button is press
 */
const CreateRollCall = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();

  const laoId = RollCallHooks.useCurrentLaoId();
  const isConnected = RollCallHooks.useConnectedToLao();

  const [proposedStartTime, setProposedStartTime] = useState(Timestamp.EpochNow());
  const [proposedEndTime, setProposedEndTime] = useState(
    Timestamp.EpochNow().addSeconds(DEFAULT_ROLL_CALL_DURATION),
  );

  const [rollCallName, setRollCallName] = useState('');
  const [rollCallLocation, setRollCallLocation] = useState('');
  const [rollCallDescription, setRollCallDescription] = useState('');
  const [modalEndIsVisible, setModalEndIsVisible] = useState(false);
  const [modalStartIsVisible, setModalStartIsVisible] = useState(false);

  const buildDatePicker = () => {
    const startDate = proposedStartTime.toDate();
    const endDate = proposedEndTime.toDate();

    return (
      <>
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.roll_call_create_proposed_start}
        </Text>
        <DatePicker
          selected={startDate}
          onChange={(date: Date) =>
            onChangeStartTime(
              date,
              setProposedStartTime,
              setProposedEndTime,
              DEFAULT_ROLL_CALL_DURATION,
            )
          }
        />

        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.roll_call_create_proposed_end}
        </Text>
        <DatePicker
          selected={endDate}
          onChange={(date: Date) => onChangeEndTime(date, proposedStartTime, setProposedEndTime)}
        />
      </>
    );
  };

  const confirmButtonEnabled: boolean =
    isConnected === true && rollCallName !== '' && rollCallLocation !== '';

  const createRollCall = () => {
    const description = rollCallDescription === '' ? undefined : rollCallDescription;
    requestCreateRollCall(
      laoId,
      rollCallName,
      rollCallLocation,
      proposedStartTime,
      proposedEndTime,
      description,
    )
      .then(() => {
        navigation.navigate(STRINGS.navigation_lao_events_home);
      })
      .catch((err) => {
        console.error('Could not create roll call, error:', err);
        toast.show(`Could not create roll call, error: ${err}`, {
          type: 'danger',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      });
  };

  const toolbarItems: ToolbarItem[] = [
    {
      id: 'roll_call_confirm_selector',
      title: STRINGS.general_button_confirm,
      disabled: !confirmButtonEnabled,
      onPress: () =>
        onConfirmEventCreation(
          proposedStartTime,
          proposedEndTime,
          createRollCall,
          setModalStartIsVisible,
          setModalEndIsVisible,
        ),
    },
  ];

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.roll_call_create_name}
      </Text>
      <Input
        value={rollCallName}
        onChange={setRollCallName}
        placeholder={STRINGS.roll_call_create_name_placeholder}
        testID="roll_call_name_selector"
      />
      <Text style={[Typography.paragraph, Typography.important]}>{STRINGS.roll_call_location}</Text>
      <Input
        value={rollCallLocation}
        onChange={setRollCallLocation}
        placeholder={STRINGS.roll_call_create_location_placeholder}
        testID="roll_call_location_selector"
      />
      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.roll_call_description}
      </Text>
      <Input
        value={rollCallDescription}
        onChange={setRollCallDescription}
        placeholder={STRINGS.roll_call_create_description_placeholder}
      />
      {buildDatePicker()}
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
      />
    </ScreenWrapper>
  );
};

export default CreateRollCall;

export const CreateRollCallScreen: RollCallFeature.LaoEventScreen = {
  id: STRINGS.events_create_roll_call,
  Component: CreateRollCall,
};
