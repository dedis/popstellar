import React from 'react';
import { Text, View, Modal, StyleSheet } from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';

import { PoPButton, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import { ModalStyles, Typography, Spacing, Color } from 'core/styles';
import STRINGS from 'resources/strings';

interface QRCodeModalProps {
  visible: boolean;
  onClose: () => void;
  qrCodeData: string;
  isInitiatingOrganizer: boolean;
  onNext: () => void;
}

const styles = StyleSheet.create({
  modalContainer: {
    ...ModalStyles.modalContainer,
  },
  marginTopSpacingX2: {
    marginTop: Spacing.x2,
  },
  textAlignCenter: {
    textAlign: 'center',
  },
});

const QRCodeModal: React.FC<QRCodeModalProps> = ({
  visible,
  onClose,
  qrCodeData,
  isInitiatingOrganizer,
  onNext,
}) => {
  return (
    <Modal testID="modal-show-qr-code" transparent visible={visible} onRequestClose={onClose}>
      <TouchableWithoutFeedback containerStyle={ModalStyles.modalBackground} onPress={onClose} />
      <View style={styles.modalContainer}>
        <ModalHeader onClose={onClose}>
          {STRINGS.linked_organizations_addlinkedorg_genQRCode}
        </ModalHeader>
        <Text style={{ ...Typography.paragraph, ...styles.textAlignCenter }}>
          {STRINGS.linked_organizations_addlinkedorg_QRCode_info}
        </Text>
        <QRCode
          value={qrCodeData}
          overlayText={STRINGS.linked_organizations_addlinkedorg_QRCode_overlay}
        />
        <View style={styles.marginTopSpacingX2}>
          <PoPButton
            testID="button-next-finish"
            buttonStyle="primary"
            onPress={onNext}
            disabled={false}>
            <Text style={{ color: Color.contrast, ...styles.textAlignCenter }}>
              {!isInitiatingOrganizer
                ? STRINGS.linked_organizations_addlinkedorg_next
                : STRINGS.linked_organizations_addlinkedorg_finished}
            </Text>
          </PoPButton>
        </View>
      </View>
    </Modal>
  );
};

export default QRCodeModal;
