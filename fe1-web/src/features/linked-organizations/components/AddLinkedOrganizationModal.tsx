import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Text, View, Modal, StyleSheet, ViewStyle } from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';
import { PoPButton } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import { Spacing, Typography, Color, ModalStyles } from 'core/styles';
import STRINGS from 'resources/strings';
import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LinkedOrganizationsParamList } from 'core/navigation/typing/LinkedOrganizationsParamList';
import { useToast } from 'react-native-toast-notifications';
import { LinkedOrganizationsHooks } from '../hooks';
import { makeChallengeSelector } from '../reducer';
import { useSelector } from 'react-redux';
import { LinkedOrganization } from '../objects/LinkedOrganization';
import { Timestamp } from 'core/objects';
import { expectFederation, initFederation, requestChallenge } from '../network';
import { Challenge } from '../objects/Challenge';
import { FOUR_SECONDS } from 'resources/const';
import QRCodeModal from './QRCodeModal';
import QRCodeScannerModal from './QRCodeScannerModal';
import ManualInputModal from './ManualInputModal';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<
    LinkedOrganizationsParamList,
    typeof STRINGS.linked_organizations_navigation_addlinkedorgModal
  >,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_linked_organizations>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const styles = StyleSheet.create({
  modalContainer: {
    ...ModalStyles.modalContainer,
  },
  marginBottomSpacingX1: {
    marginBottom: Spacing.x1,
  },
  infoText: {
    textAlign: 'center',
    fontSize: 18,
    margin: 3,
    color: Color.contrast,
  },
});

const AddLinkedOrganizationModal = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  const lao = LinkedOrganizationsHooks.useCurrentLao();
  const challengeSelector = useMemo(() => makeChallengeSelector(laoId), [laoId]);
  const challengeState = useSelector(challengeSelector);

  const [organizations, setOrganizations] = useState<LinkedOrganization[]>([]);
  const [showModal, setShowModal] = useState<boolean>(true);

  const [showQRScannerModal, setShowQRScannerModal] = useState<boolean>(false);
  const [showQRCodeModal, setShowQRCodeModal] = useState<boolean>(false);

  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);

  const [qrCodeData, setQRCodeData] = useState<string>('');
  // Check if the organizer is the one thats supposed to send the initFederation message
  const [isInitiatingOrganizer, setIsInitiatingOrganizer] = useState<boolean>(false);
  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);

  const onRequestChallenge = useCallback(() => {
    console.log('Requesting challenge');
    requestChallenge(laoId)
      .then(() => {
        console.log('Success: Requesting challenge');
      })
      .catch((err) => {
        console.error('Could not request Challenge, error:', err);
      });
  }, [laoId]);

  const getQRCodeData = () => {
    if (!isInitiatingOrganizer) {
      onRequestChallenge();
    } else {
      const jsonObj = {
        lao_id: laoId,
        server_address: lao.server_addresses.at(0),
        public_key: lao.organizer,
      };
      setQRCodeData(JSON.stringify(jsonObj));
      console.log('qrCodeData:', jsonObj);
    }
  };

  const onFederationExpect = useCallback(
    (org: LinkedOrganization) => {
      console.log('Expect Federation');
      if (challengeState) {
        expectFederation(
          laoId,
          org.lao_id,
          org.server_address,
          org.public_key,
          Challenge.fromState(challengeState),
        )
          .then(() => {
            console.log('Success: Expect Federation');
          })
          .catch((err) => {
            console.error('Could not expect Federation, error:', err);
          });
      }
    },
    [laoId, challengeState],
  );

  const onFederationInit = useCallback(
    (org: LinkedOrganization) => {
      console.log('Init Federation');
      initFederation(laoId, org.lao_id, org.server_address, org.public_key, org.challenge!)
        .then(() => {
          console.log('Success: Init Federation');
        })
        .catch((err) => {
          console.error('Could not init Federation, error:', err);
        });
    },
    [laoId],
  );

  const onScanData = (qrCode: string | null) => {
    console.log(qrCode);
    const qrcode_checked = qrCode ?? '';
    try {
      // Data of the Linked Organization that was just scanned
      const scannedLinkedOrganization = LinkedOrganization.fromJson(JSON.parse(qrcode_checked));
      setOrganizations([...organizations, scannedLinkedOrganization]);
      setShowScanner(false);
      setShowQRScannerModal(false);
      if (isInitiatingOrganizer) {
        getQRCodeData();
        setShowQRCodeModal(true);
        onFederationInit(scannedLinkedOrganization);
      } else {
        onFederationExpect(scannedLinkedOrganization);
        navigation.navigate(STRINGS.navigation_linked_organizations);
      }
      toast.show(`QR Code successfully scanned`, {
        type: 'success',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    } catch (error) {
      toast.show(`Could not scan QR Code, error: ${error}`, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
      console.log(error);
    }
  };

  useEffect(() => {
    if (challengeState) {
      const challenge = Challenge.fromState(challengeState);
      const jsonObj = {
        lao_id: laoId,
        server_address: lao.server_addresses.at(0),
        public_key: lao.organizer,
        challenge: {
          value: challenge.value,
          valid_until: challenge.valid_until,
        },
      };
      setQRCodeData(JSON.stringify(jsonObj));
      console.log('qrCodeData:', jsonObj);
    }
  }, [challengeState, laoId, lao.organizer, lao.server_addresses]);

  return (
    <>
      <Modal
        testID="modal-add-organization"
        transparent
        visible={showModal}
        onRequestClose={() => {
          setShowModal(false);
          setIsInitiatingOrganizer(false);
        }}>
        <TouchableWithoutFeedback
          containerStyle={ModalStyles.modalBackground}
          onPress={() => {
            setShowModal(false);
            setIsInitiatingOrganizer(false);
          }}
        />
        <View style={styles.modalContainer}>
          <ModalHeader
            onClose={() => {
              setShowModal(false);
              setIsInitiatingOrganizer(false);
              navigation.goBack()
            }}>
            {STRINGS.linked_organizations_addlinkedorg_title}
          </ModalHeader>
          <Text style={Typography.paragraph}>{STRINGS.linked_organizations_addlinkedorg_info}</Text>
          <View style={styles.marginBottomSpacingX1}>
            <PoPButton
              testID="show-qr-code"
              onPress={() => {
                setShowQRCodeModal(true);
                setIsInitiatingOrganizer(false);
                getQRCodeData();
                setShowModal(false);
              }}
              buttonStyle="primary"
              disabled={false}>
              <Text style={styles.infoText}>
                {STRINGS.linked_organizations_addlinkedorg_genQRCode}
              </Text>
            </PoPButton>
          </View>
          <PoPButton
            testID="show-scanner"
            buttonStyle="tertiary"
            onPress={() => {
              setShowQRScannerModal(true);
              setShowScanner(true);
              setIsInitiatingOrganizer(true);
              setShowModal(false);
            }}
            disabled={false}>
            <Text style={styles.infoText}>
              {STRINGS.linked_organizations_addlinkedorg_scanQRCode}
            </Text>
          </PoPButton>
        </View>
      </Modal>

      <QRCodeModal
        visible={showQRCodeModal}
        onClose={() => {
            setShowQRCodeModal(false);
            navigation.navigate(STRINGS.navigation_linked_organizations);
        }}
        qrCodeData={qrCodeData}
        isInitiatingOrganizer={isInitiatingOrganizer}
        onNext={() => {
          setShowQRCodeModal(false);
          if (!isInitiatingOrganizer) {
            setShowQRScannerModal(true);
            setShowScanner(true);
          } else {
            navigation.navigate(STRINGS.navigation_linked_organizations);
          }
        }}
      />
      <QRCodeScannerModal
        visible={showQRScannerModal}
        onClose={() => {
          setShowQRScannerModal(false);
          setShowScanner(false);
          setIsInitiatingOrganizer(false);
          navigation.navigate(STRINGS.navigation_linked_organizations);
        }}
        showScanner={showScanner}
        onScanData={onScanData}
        onOpenManualInput={() => setInputModalIsVisible(true)}
      />

      <ManualInputModal
        visible={inputModalIsVisible}
        onClose={() => setInputModalIsVisible(false)}
        isInitiatingOrganizer={isInitiatingOrganizer}
        onScanData={onScanData}
      />
    </>
  );
};

export default AddLinkedOrganizationModal;
