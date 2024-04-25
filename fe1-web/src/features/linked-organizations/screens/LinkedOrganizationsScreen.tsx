import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem, FAB } from '@rneui/themed';
import React, { useState } from 'react';
import { Text, View, Modal, TouchableOpacity, StyleSheet, ViewStyle, ScrollView} from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LinkedOrganizationsParamList } from 'core/navigation/typing/LinkedOrganizationsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Border, List, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';
import { LinkedOrganizationsHooks } from '../hooks';
import { Organization } from '../objects/Organization';
import { accent, contrast } from 'core/styles/color';
import { ConfirmModal, DatePicker, Input, PoPButton, PoPIcon, PoPTextButton, QRCode } from 'core/components';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { Color } from 'core/styles';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import { container } from 'core/styles/list';
import { useToast } from 'react-native-toast-notifications';
import { FOUR_SECONDS } from 'resources/const';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import { Challenge } from '../objects/Challenge';
import { Hash, Timestamp } from 'core/objects';


type NavigationProps = CompositeScreenProps<
  StackScreenProps<LinkedOrganizationsParamList, typeof STRINGS.navigation_linked_organizations>,
  CompositeScreenProps<
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>,
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_linked_organizations>
  >
>;


const initialOrganizations: Organization[] = [];


const styles = StyleSheet.create({
  buttonContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  leftButtons: QrCodeScannerUIElementContainer,
  qrCode: {
      alignItems: 'center',
      justifyContent: 'center',
      opacity: 0.5,
      top: '50%',
      bottom: '50%',
  } as ViewStyle,
container: {
    flex: 1,
    justifyContent: 'space-between',
    marginVertical: Spacing.contentSpacing,
  } as ViewStyle,
scannerTextItems: {
  top: '105%',
  } as ViewStyle,
enterButton: {
    ...QrCodeScannerUIElementContainer,
    borderColor: Color.blue,
    borderWidth: Spacing.x025,
    alignSelf: 'center',
    marginBottom: Spacing.contentSpacing,
  } as ViewStyle,
inputContainer: {
    flex: 1,
    flexDirection: 'row',
} as ViewStyle,
input: {
  // this makes the input field shrink down to a width of 50
  width: 50,
  flex: 1,
  backgroundColor: Color.contrast,
  borderRadius: Border.inputRadius,
  borderWidth: Border.width,
  borderColor: Color.contrast,
  padding: Spacing.x05,
},
});

const LinkedOrganizationsScreen = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();

  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);

  const [organizations, setOrganizations] = useState<Organization[]>(initialOrganizations);
  const [showModal, setShowModal] = useState<boolean>(false);

  const [showQRScannerModal, setShowQRScannerModal] = useState<boolean>(false);
  const [showQRCodeModal, setShowQRCodeModal] = useState<boolean>(false);

  const [inputModalIsVisible, setInputModalIsVisible] = useState(false);

  const [qrCodeData, setQrCodeData] = useState<string>("");
  const sampleJsonString = `{
    "lao_id": "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=",
    "public_key": "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
    "server_adress": "wss://epfl.ch:9000/server",
    "challenge": {
      "value": "82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d",
      "valid_until": 1714491502
  }
}`;
  const [genQrCodeData, setGenQrCodeData] = useState<JSON>(JSON.parse(sampleJsonString));
  const [isClientA, setIsClientA] = useState<boolean>(false);
  const [manualLaoId, setManualLaoID] = useState<string>('');
  const [manualPublicKey, setManualPublicKey] = useState<string>('');
  const [manualServerAddress, setManualServerAddress] = useState<string>('');
  const [manualChallengeValue, setManualChallengeValue] = useState<string>('');
  const [manualChallengeValidUntil, setManualChallengeValidUntil] = useState<Timestamp>(Timestamp.EpochNow());

  const [startDate, setStartDate] = useState(Timestamp.EpochNow().toDate());

  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);

  const onScanData = (qrCode: string | null) => {
    console.log(qrCode)
    if (qrCode == null) {
      qrCode = "";
    }
    setQrCodeData(qrCode);
    try {
      let org1 = Organization.fromJson(JSON.parse(qrCode));
      console.log(org1.toJson());
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


  return (
    <View style={{flex: 1}}>
      {isOrganizer && (
    <><ScreenWrapper>
          <Text style={Typography.paragraph}>{STRINGS.linked_organizations_description}</Text>
          <View style={List.container}>
            {organizations.map((organization, idx) => {
              const listStyle = List.getListItemStyles(
                true && idx === 0,
                idx === organizations.length - 1
              );

              return (
                <ListItem
                  containerStyle={listStyle}
                  style={listStyle}
                  bottomDivider>
                  <PoPIcon name="business" />
                  <ListItem.Content>
                    <ListItem.Title>ID: {organization.lao_id}</ListItem.Title>
                    <ListItem.Subtitle>Public Key: {organization.public_key}, Server Address: {organization.server_address}, Challenge Value: {organization.challenge.value}, Challenge Valid_until: {organization.challenge.valid_until.valueOf()}</ListItem.Subtitle>
                  </ListItem.Content>
                </ListItem>
              );
            })}
          </View>



          <Modal
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
          <Text style={Typography.paragraph}>{STRINGS.linked_organizations_addlinkedorg_info}</Text>
          <View style={{ marginBottom: 10 }}>
          <PoPButton
          onPress={() => {
            setShowQRCodeModal(!showQRCodeModal);
            setShowModal(!showModal);
            setIsClientA(true)
          }}
          buttonStyle={'primary'}
            disabled={false}>
            <Text style={{ color: contrast, textAlign: 'center', fontSize: 18, margin: 3}}>{STRINGS.linked_organizations_addlinkedorg_genQRCode}</Text>
          </PoPButton>
          </View>
          <PoPButton
            buttonStyle={'tertiary'}
            onPress={() => {
              setShowQRScannerModal(!showQRScannerModal);
              setShowModal(!showModal);
              setShowScanner(!showScanner);
              setIsClientA(false)
            }}
            disabled={false}>
            <Text style={{ color: contrast, textAlign: 'center', fontSize: 18, margin: 3}}>{STRINGS.linked_organizations_addlinkedorg_scanQRCode}</Text>
          </PoPButton>
        </View>
      </Modal>


      <Modal
        transparent
        visible={showQRScannerModal}
        onRequestClose={() => {
          setShowQRScannerModal(!showQRScannerModal);
          setShowScanner(false);
        }}
        style={{flex: 1}}
        >
        <TouchableWithoutFeedback
          containerStyle={ModalStyles.modalBackground}
          onPress={() => {
            setShowQRScannerModal(!showQRScannerModal);
            setShowScanner(false);
          }}
        />
        <View style={{...ModalStyles.modalContainer, flex:1}}>
          <ModalHeader onClose={() => {
            setShowQRScannerModal(!showQRScannerModal); 
            setShowScanner(false);}
          }>
            {STRINGS.linked_organizations_addlinkedorg_scanQRCode}
          </ModalHeader>
          <QrCodeScanner showCamera={showScanner} handleScan={onScanData}>
          <View style={container}>
          <View style={styles.qrCode}>
            <QrCodeScanOverlay width={300} height={300} />
          </View>
          <View style={styles.scannerTextItems}>
            <View style={styles.enterButton}>
              <PoPTouchableOpacity
                testID="roll_call_open_add_manually"
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
          <Text style={{ ...Typography.paragraph, textAlign: 'center'}}>{STRINGS.linked_organizations_addlinkedorg_QRCode_info}</Text>
          <QRCode
              value={JSON.stringify(genQrCodeData)}
              overlayText={STRINGS.linked_organizations_addlinkedorg_QRCode_overlay}
          />
          <View style={{ marginTop: 15 }}>
          <PoPButton
            buttonStyle={'primary'}
            onPress={() => {
              setShowQRCodeModal(!showQRCodeModal);
              if (isClientA) {
                setShowQRScannerModal(!showQRScannerModal);
                setShowScanner(!showScanner);
              }
            }}
            disabled={false}>
            <Text style={{ color: contrast, textAlign: 'center'}}>
            {isClientA ? STRINGS.linked_organizations_addlinkedorg_next : STRINGS.linked_organizations_addlinkedorg_finished}
            </Text>
          </PoPButton>
          </View>
        </View>
      </Modal>


      <Modal
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

          <Text style={Typography.paragraph}>{STRINGS.linked_organizations_enterChallengeValidUntil}</Text>
          <DatePicker
          selected={startDate}
          onChange={(date: Date) => {
            setStartDate(date);
            setManualChallengeValidUntil(Timestamp.fromDate(date));
          }
          }
        />
          
          <PoPTextButton onPress={() => {
             if (!manualLaoId || !manualPublicKey || !manualServerAddress || !manualChallengeValue || manualChallengeValidUntil <= Timestamp.EpochNow()) {
              toast.show(`All fields are required and Valid Until has to be in the Future`, {
                type: 'danger',
                placement: 'bottom',
                duration: FOUR_SECONDS,
              });
              return;
            }
            
            let tmpOrg = new Organization({lao_id: new Hash(manualLaoId), 
              public_key: new Hash(manualPublicKey), 
              server_address: manualServerAddress, 
              challenge: new Challenge({value: new Hash(manualChallengeValue), valid_until: manualChallengeValidUntil})
            });
            console.log(tmpOrg.toJson());
            onScanData(tmpOrg.toJson());
            setInputModalIsVisible(!inputModalIsVisible);
          }}>
            {STRINGS.general_add}
          </PoPTextButton>
        </ScrollView>
      </Modal>


        </ScreenWrapper><FAB
            placement="right"
            color={accent}
            onPress={() => {
                setShowModal(true);
              }}
            icon={{ name: 'add', color: contrast }}
            style={{ position: 'absolute', bottom: 30, right: 30 }} /></>)}
    </View>
  );
};

export default LinkedOrganizationsScreen;
