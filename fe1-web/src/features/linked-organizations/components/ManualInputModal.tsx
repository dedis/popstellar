import React, { useState } from 'react';
import { Text, Modal, ScrollView } from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';
import { useToast } from 'react-native-toast-notifications';

import { DatePicker, Input, PoPTextButton } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import { Timestamp, Hash } from 'core/objects';
import { ModalStyles, Typography } from 'core/styles';
import { FOUR_SECONDS, DAY_IN_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { Challenge } from '../objects/Challenge';

interface ManualInputModalProps {
  visible: boolean;
  onClose: () => void;
  isInitiatingOrganizer: boolean;
  onScanData: (qrCode: string | null) => void;
}

const ManualInputModal: React.FC<ManualInputModalProps> = ({
  visible,
  onClose,
  isInitiatingOrganizer,
  onScanData,
}) => {
  const toast = useToast();
  const [manualLaoId, setManualLaoID] = useState<string>('');
  const [manualPublicKey, setManualPublicKey] = useState<string>('');
  const [manualServerAddress, setManualServerAddress] = useState<string>('');
  const [manualChallengeValue, setManualChallengeValue] = useState<string>('');
  const [manualChallengeValidUntil, setManualChallengeValidUntil] = useState<Timestamp>(
    Timestamp.EpochNow().addSeconds(DAY_IN_SECONDS),
  );
  const [startDate, setStartDate] = useState(manualChallengeValidUntil.toDate());

  return (
    <Modal testID="modal-manual-input" transparent visible={visible} onRequestClose={onClose}>
      <TouchableWithoutFeedback containerStyle={ModalStyles.modalBackground} onPress={onClose} />
      <ScrollView style={ModalStyles.modalContainer}>
        <ModalHeader onClose={onClose}>
          {STRINGS.linked_organizations_addmanually_title}
        </ModalHeader>
        <Text style={Typography.paragraph}>{STRINGS.linked_organizations_enterLaoId}</Text>
        <Input
          testID="modal-input-laoid"
          value={manualLaoId}
          onChange={setManualLaoID}
          placeholder={STRINGS.linked_organizations_placeholderLaoID}
        />
        <Text style={Typography.paragraph}>{STRINGS.linked_organizations_enterPublicKey}</Text>
        <Input
          testID="modal-input-publickey"
          value={manualPublicKey}
          onChange={setManualPublicKey}
          placeholder={STRINGS.linked_organizations_placeholderPublicKey}
        />
        <Text style={Typography.paragraph}>{STRINGS.linked_organizations_enterServerAddress}</Text>
        <Input
          testID="modal-input-serveraddress"
          value={manualServerAddress}
          onChange={setManualServerAddress}
          placeholder={STRINGS.linked_organizations_placeholderServerAddress}
        />
        <Text style={Typography.paragraph}>{STRINGS.linked_organizations_enterChallengeValue}</Text>
        <Input
          testID="modal-input-challengeval"
          value={manualChallengeValue}
          onChange={setManualChallengeValue}
          placeholder={STRINGS.linked_organizations_placeholderChallengeValue}
        />

        <Text style={Typography.paragraph}>
          {STRINGS.linked_organizations_enterChallengeValidUntil}
        </Text>
        <DatePicker
          selected={startDate}
          onChange={(date: Date) => {
            setStartDate(date);
            setManualChallengeValidUntil(Timestamp.fromDate(date));
          }}
        />

        <PoPTextButton
          testID="add-manually"
          onPress={() => {
            if (
              (isInitiatingOrganizer &&
                (!manualLaoId ||
                  !manualPublicKey ||
                  !manualServerAddress ||
                  !manualChallengeValue ||
                  manualChallengeValidUntil <= Timestamp.EpochNow())) ||
              (!isInitiatingOrganizer && (!manualLaoId || !manualPublicKey || !manualServerAddress))
            ) {
              toast.show(STRINGS.linked_organizations_manualInputModalToast, {
                type: 'danger',
                placement: 'bottom',
                duration: FOUR_SECONDS,
              });
              return;
            }

            const tmpOrg = isInitiatingOrganizer
              ? {
                  lao_id: new Hash(manualLaoId),
                  public_key: new Hash(manualPublicKey),
                  server_address: manualServerAddress,
                  challenge: new Challenge({
                    value: new Hash(manualChallengeValue),
                    valid_until: manualChallengeValidUntil,
                  }),
                }
              : {
                  lao_id: new Hash(manualLaoId),
                  public_key: new Hash(manualPublicKey),
                  server_address: manualServerAddress,
                };
            onScanData(JSON.stringify(tmpOrg));
            onClose();
            setManualLaoID('');
            setManualPublicKey('');
            setManualServerAddress('');
            setManualChallengeValue('');
            setManualChallengeValidUntil(Timestamp.EpochNow());
          }}>
          {STRINGS.general_add}
        </PoPTextButton>
      </ScrollView>
    </Modal>
  );
};

export default ManualInputModal;
