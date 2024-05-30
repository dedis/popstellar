import React from 'react';
import { Text, View, Modal, StyleSheet, ViewStyle } from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';

import ModalHeader from 'core/components/ModalHeader';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import { ModalStyles, Typography, Spacing, Color } from 'core/styles';
import STRINGS from 'resources/strings';

interface QRCodeScannerModalProps {
  visible: boolean;
  onClose: () => void;
  showScanner: boolean;
  onScanData: (qrCode: string | null) => void;
  onOpenManualInput: () => void;
}

const styles = StyleSheet.create({
  modalContainer: {
    ...ModalStyles.modalContainer,
    flex: 1,
  },
  qrCode: {
    alignItems: 'center',
    justifyContent: 'center',
    opacity: 0.5,
    top: '25%',
    bottom: '50%',
  } as ViewStyle,
  scannerTextItems: {
    top: '35%',
  } as ViewStyle,
  openManuallyView: {
    ...QrCodeScannerUIElementContainer,
    borderColor: Color.blue,
    borderWidth: Spacing.x025,
    alignSelf: 'center',
    marginBottom: Spacing.contentSpacing,
  } as ViewStyle,
  textAlignCenter: {
    textAlign: 'center',
  },
});

const QRCodeScannerModal: React.FC<QRCodeScannerModalProps> = ({
  visible,
  onClose,
  showScanner,
  onScanData,
  onOpenManualInput,
}) => {
  return (
    <Modal
      testID="modal-show-scanner"
      transparent
      visible={visible}
      onRequestClose={onClose}
      style={styles.modalContainer}>
      <TouchableWithoutFeedback containerStyle={ModalStyles.modalBackground} onPress={onClose} />
      <View style={styles.modalContainer}>
        <ModalHeader onClose={onClose}>
          {STRINGS.linked_organizations_addlinkedorg_scanQRCode}
        </ModalHeader>
        <Text style={{ ...Typography.paragraph, ...styles.textAlignCenter }}>
          {STRINGS.linked_organizations_addlinkedorg_Scanner_info}
        </Text>
        <QrCodeScanner showCamera={showScanner} handleScan={onScanData}>
          <View>
            <View style={styles.qrCode}>
              <QrCodeScanOverlay width={300} height={300} />
            </View>
            <View style={styles.scannerTextItems}>
              <View style={styles.openManuallyView}>
                <PoPTouchableOpacity testID="open_add_manually" onPress={onOpenManualInput}>
                  <Text style={[Typography.base, Typography.accent, Typography.centered]}>
                    {STRINGS.general_enter_manually}
                  </Text>
                </PoPTouchableOpacity>
              </View>
            </View>
          </View>
        </QrCodeScanner>
      </View>
    </Modal>
  );
};

export default QRCodeScannerModal;
