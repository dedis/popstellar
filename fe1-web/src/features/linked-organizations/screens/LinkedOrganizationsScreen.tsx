import { ListItem, FAB } from '@rneui/themed';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Text, View, Modal, StyleSheet, ViewStyle, ScrollView } from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';
import { useToast } from 'react-native-toast-notifications';

import { DatePicker, Input, PoPButton, PoPIcon, PoPTextButton, QRCode } from 'core/components';
import ModalHeader from 'core/components/ModalHeader';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Hash, Timestamp } from 'core/objects';
import { List, ModalStyles, Spacing, Typography, Color } from 'core/styles';
import { accent, contrast } from 'core/styles/color';
import { container } from 'core/styles/list';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { LinkedOrganizationsHooks } from '../hooks';
import { Challenge } from '../objects/Challenge';
import { Organization } from '../objects/Organization';
import { Lao } from 'features/lao/objects';
import { requestChallenge } from '../network';
import { makeChallengeSelector } from '../reducer';
import { useSelector } from 'react-redux';

const initialOrganizations: Organization[] = [];

const styles = StyleSheet.create({
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
  enterButton: {
    ...QrCodeScannerUIElementContainer,
    borderColor: Color.blue,
    borderWidth: Spacing.x025,
    alignSelf: 'center',
    marginBottom: Spacing.contentSpacing,
  } as ViewStyle,
  flex1: {
    flex: 1,
  },
  marginB10: {
    marginBottom: 10,
  },
  marginT15: {
    marginTop: 15,
  },
  textAlignCenter: {
    textAlign: 'center',
  },
  infoText: {
    textAlign: 'center',
    fontSize: 18,
    margin: 3,
    color: contrast,
  },
  fabStyle: {
    position: 'absolute',
    bottom: 30,
    right: 30,
  },
});

const LinkedOrganizationsScreen = () => {
  const toast = useToast();
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();
  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);
  const lao = LinkedOrganizationsHooks.useCurrentLao();
  const challengeSelector = useMemo(() => makeChallengeSelector(laoId), [laoId]);
  const challengeState = useSelector(challengeSelector);
  const [isRequested, setIsRequested] = useState<boolean>(false);

  const [organizations, setOrganizations] = useState<Organization[]>(initialOrganizations);
  const [showModal, setShowModal] = useState<boolean>(false);

  const [showQRScannerModal, setShowQRScannerModal] = useState<boolean>(false);
  const [showQRCodeModal, setShowQRCodeModal] = useState<boolean>(false);

  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);


  const [qrCodeData, setQRCodeData] = useState<string>('');
  const [isClientA, setIsClientA] = useState<boolean>(false);
  const [manualLaoId, setManualLaoID] = useState<string>('');
  const [manualPublicKey, setManualPublicKey] = useState<string>('');
  const [manualServerAddress, setManualServerAddress] = useState<string>('');
  const [manualChallengeValue, setManualChallengeValue] = useState<string>('');
  const [manualChallengeValidUntil, setManualChallengeValidUntil] = useState<Timestamp>(
    Timestamp.EpochNow().addSeconds(86400),
  );
  const [startDate, setStartDate] = useState(manualChallengeValidUntil.toDate());

  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);

  const onScanData = (qrCode: string | null) => {
    const qrcode1 = qrCode ?? '';
    try {
      const org1 = Organization.fromJson(JSON.parse(qrcode1));
      setOrganizations([...organizations, org1]);
      setShowScanner(false);
      setShowQRScannerModal(!showQRScannerModal);
      if (!isClientA) {
        setShowQRCodeModal(!showQRCodeModal);
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
    }
  };

  const onRequestChallenge = useCallback(() => {
    console.log('Requesting challenge');
    requestChallenge(laoId)
      .then(() => {
        console.log('Success: Requesting challenge');
        setIsRequested(true);
      })
      .catch((err) => {
        console.error('Could not request Challenge, error:', err);
      });
  }, [laoId]);

  useEffect(() => {
    if (challengeState && isRequested) {
      console.log("challengeState:", challengeState);
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
      console.log("qrCodeData:", jsonObj);
    }
  }, [challengeState, laoId]);

  const getQRCodeData = () => {
    if (!isClientA) {
      onRequestChallenge();
    } else {
      const jsonObj = {
        lao_id: laoId,
        server_address: lao.server_addresses.at(0),
        public_key: lao.organizer,
      };
      setQRCodeData(JSON.stringify(jsonObj));
      console.log("qrCodeData:")
      console.log(jsonObj);
    }
  }

  return (
    <View style={styles.flex1}>
      {isOrganizer && (
        <>
          <ScreenWrapper>
            <Text style={Typography.paragraph}>{STRINGS.linked_organizations_description}</Text>
            <View style={List.container}>
              {organizations.map((organization, idx) => {
                const listStyle = List.getListItemStyles(
                  true && idx === 0,
                  idx === organizations.length - 1,
                );

                return (
                  <ListItem
                    containerStyle={listStyle}
                    style={listStyle}
                    bottomDivider
                    key={organization.lao_id.valueOf()}>
                    <PoPIcon name="business" />
                    <ListItem.Content>
                      <ListItem.Title>Lao ID: {organization.lao_id}</ListItem.Title>
                      <ListItem.Subtitle>
                        Public Key: {organization.public_key}, Server Address:{' '}
                        {organization.server_address}, Challenge Value:{' '}
                        {organization.challenge.value}, Challenge Valid_until:{' '}
                        {organization.challenge.valid_until.valueOf()}
                      </ListItem.Subtitle>
                    </ListItem.Content>
                  </ListItem>
                );
              })}
            </View>

            <Modal
              testID="modal-add-organization"
              transparent
              visible={showModal}
              onRequestClose={() => {
                setShowModal(!showModal);
              }}>
              <TouchableWithoutFeedback
                containerStyle={ModalStyles.modalBackground}
                onPress={() => {
                  setShowModal(!showModal);
                }}
              />
              <View style={ModalStyles.modalContainer}>
                <ModalHeader onClose={() => setShowModal(!showModal)}>
                  {STRINGS.linked_organizations_addlinkedorg_title}
                </ModalHeader>
                <Text style={Typography.paragraph}>
                  {STRINGS.linked_organizations_addlinkedorg_info}
                </Text>
                <View style={styles.marginB10}>
                  <PoPButton
                    testID="show-qr-code"
                    onPress={() => {
                      setShowQRCodeModal(!showQRCodeModal);
                      setShowModal(!showModal);
                      setIsClientA(!isClientA);
                      getQRCodeData();
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
                    setShowQRScannerModal(!showQRScannerModal);
                    setShowModal(!showModal);
                    setShowScanner(!showScanner);
                    setIsClientA(!isClientA);
                  }}
                  disabled={false}>
                  <Text style={styles.infoText}>
                    {STRINGS.linked_organizations_addlinkedorg_scanQRCode}
                  </Text>
                </PoPButton>
              </View>
            </Modal>

            <Modal
              testID="modal-show-scanner"
              transparent
              visible={showQRScannerModal}
              onRequestClose={() => {
                setShowQRScannerModal(!showQRScannerModal);
                setShowScanner(false);
              }}
              style={styles.flex1}>
              <TouchableWithoutFeedback
                containerStyle={ModalStyles.modalBackground}
                onPress={() => {
                  setShowQRScannerModal(!showQRScannerModal);
                  setShowScanner(false);
                }}
              />
              <View style={{ ...ModalStyles.modalContainer, ...styles.flex1 }}>
                <ModalHeader
                  onClose={() => {
                    setShowQRScannerModal(!showQRScannerModal);
                    setShowScanner(false);
                  }}>
                  {STRINGS.linked_organizations_addlinkedorg_scanQRCode}
                </ModalHeader>
                <Text style={{ ...Typography.paragraph, ...styles.textAlignCenter }}>
                  {STRINGS.linked_organizations_addlinkedorg_Scanner_info}
                </Text>
                <QrCodeScanner showCamera={showScanner} handleScan={onScanData}>
                  <View style={container}>
                    <View style={styles.qrCode}>
                      <QrCodeScanOverlay width={300} height={300} />
                    </View>
                    <View style={styles.scannerTextItems}>
                      <View style={styles.enterButton}>
                        <PoPTouchableOpacity
                          testID="open_add_manually"
                          onPress={() => setInputModalIsVisible(true)}>
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

            <Modal
              testID="modal-show-qr-code"
              transparent
              visible={showQRCodeModal}
              onRequestClose={() => {
                setShowQRCodeModal(!showQRCodeModal);
              }}>
              <TouchableWithoutFeedback
                containerStyle={ModalStyles.modalBackground}
                onPress={() => {
                  setShowQRCodeModal(!showQRCodeModal);
                }}
              />
              <View style={ModalStyles.modalContainer}>
                <ModalHeader onClose={() => setShowQRCodeModal(!showQRCodeModal)}>
                  {STRINGS.linked_organizations_addlinkedorg_genQRCode}
                </ModalHeader>
                <Text style={{ ...Typography.paragraph, ...styles.textAlignCenter }}>
                  {STRINGS.linked_organizations_addlinkedorg_QRCode_info}
                </Text>
                <QRCode
                  value={qrCodeData}
                  overlayText={STRINGS.linked_organizations_addlinkedorg_QRCode_overlay}
                />
                <View style={styles.marginT15}>
                  <PoPButton
                    testID="button-next-finish"
                    buttonStyle="primary"
                    onPress={() => {
                      setShowQRCodeModal(!showQRCodeModal);
                      if (isClientA) {
                        setShowQRScannerModal(!showQRScannerModal);
                        setShowScanner(!showScanner);
                      }
                    }}
                    disabled={false}>
                    <Text style={{ color: contrast, ...styles.textAlignCenter }}>
                      {isClientA
                        ? STRINGS.linked_organizations_addlinkedorg_next
                        : STRINGS.linked_organizations_addlinkedorg_finished}
                    </Text>
                  </PoPButton>
                </View>
              </View>
            </Modal>

            <Modal
              testID="modal-manual-input"
              transparent
              visible={inputModalIsVisible}
              onRequestClose={() => {
                setInputModalIsVisible(!inputModalIsVisible);
              }}>
              <TouchableWithoutFeedback
                containerStyle={ModalStyles.modalBackground}
                onPress={() => {
                  setInputModalIsVisible(!inputModalIsVisible);
                }}
              />
              <ScrollView style={ModalStyles.modalContainer}>
                <ModalHeader onClose={() => setInputModalIsVisible(!inputModalIsVisible)}>
                  {STRINGS.linked_organizations_addmanually_title}
                </ModalHeader>
                <Text style={Typography.paragraph}>{STRINGS.linked_organizations_enterLaoId}</Text>
                <Input
                  testID="modal-input-laoid"
                  value={manualLaoId}
                  onChange={setManualLaoID}
                  placeholder={STRINGS.linked_organizations_placeholderLaoID}
                />
                <Text style={Typography.paragraph}>
                  {STRINGS.linked_organizations_enterPublicKey}
                </Text>
                <Input
                  testID="modal-input-publickey"
                  value={manualPublicKey}
                  onChange={setManualPublicKey}
                  placeholder={STRINGS.linked_organizations_placeholderPublicKey}
                />
                <Text style={Typography.paragraph}>
                  {STRINGS.linked_organizations_enterServerAddress}
                </Text>
                <Input
                  testID="modal-input-serveraddress"
                  value={manualServerAddress}
                  onChange={setManualServerAddress}
                  placeholder={STRINGS.linked_organizations_placeholderServerAddress}
                />
                <Text style={Typography.paragraph}>
                  {STRINGS.linked_organizations_enterChallengeValue}
                </Text>
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
                      !manualLaoId ||
                      !manualPublicKey ||
                      !manualServerAddress ||
                      !manualChallengeValue ||
                      manualChallengeValidUntil <= Timestamp.EpochNow()
                    ) {
                      toast.show(
                        `All fields are required and Valid Until has to be in the Future`,
                        {
                          type: 'danger',
                          placement: 'bottom',
                          duration: FOUR_SECONDS,
                        },
                      );
                      return;
                    }

                    const tmpOrg = new Organization({
                      lao_id: new Hash(manualLaoId),
                      public_key: new Hash(manualPublicKey),
                      server_address: manualServerAddress,
                      challenge: new Challenge({
                        value: new Hash(manualChallengeValue),
                        valid_until: manualChallengeValidUntil,
                      }),
                    });
                    onScanData(tmpOrg.toJson());
                    setInputModalIsVisible(!inputModalIsVisible);
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
          </ScreenWrapper>
          <FAB
            testID="fab-button"
            placement="right"
            color={accent}
            onPress={() => {
              setShowModal(true);
            }}
            icon={{ name: 'add', color: contrast }}
            style={styles.fabStyle}
          />
        </>
      )}
    </View>
  );
};

export default LinkedOrganizationsScreen;
