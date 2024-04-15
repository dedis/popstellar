import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem, FAB } from '@rneui/themed';
import React, { useState } from 'react';
import { Text, View, Modal, TouchableOpacity, StyleSheet, ViewStyle} from 'react-native';
import { TouchableWithoutFeedback } from 'react-native-gesture-handler';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LinkedOrganizationsParamList } from 'core/navigation/typing/LinkedOrganizationsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { List, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';
import { LinkedOrganizationsHooks } from '../hooks';
import { Organization } from '../objects/Organizations';
import { accent, contrast } from 'core/styles/color';
import { PoPButton, PoPIcon, QRCode } from 'core/components';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { Color, Icon } from 'core/styles';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import { centered } from 'core/styles/typography';
import { container } from 'core/styles/list';




type NavigationProps = CompositeScreenProps<
  StackScreenProps<LinkedOrganizationsParamList, typeof STRINGS.navigation_linked_organizations>,
  CompositeScreenProps<
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>,
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_linked_organizations>
  >
>;


const initialOrganizations: Organization[] = [
  Organization.fromState({ laoId: "1", name: 'Linked Org 1' }),
  Organization.fromState({ laoId: "2", name: 'Linked Org 2' }),
  Organization.fromState({ laoId: "3", name: 'Linked Org 3' }),
  // Add your initial organization items here
];


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
      top: '35%',
      bottom: '50%',
      
    } as ViewStyle,
    container: {
      flex: 1,
      justifyContent: 'center',
      marginVertical: Spacing.contentSpacing,
      position: 'relative'
    } as ViewStyle,
});

const LinkedOrganizationsScreen = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const laoId = LinkedOrganizationsHooks.useCurrentLaoId();

  const isOrganizer = LinkedOrganizationsHooks.useIsLaoOrganizer(laoId);

  const [organizations, setOrganizations] = useState<Organization[]>(initialOrganizations);
  const [showModal, setShowModal] = useState<boolean>(false);

  const [showQRScannerModal, setShowQRScannerModal] = useState<boolean>(false);
  const [showQRCodeModal, setShowQRCodeModal] = useState<boolean>(false);

  const [qrCodeData, setQrCodeData] = useState<string>("");
  const [genQrCodeData, setGenQrCodeData] = useState<string>("test");
  const [isClientA, setIsClientA] = useState<boolean>(false);

  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);

  const onScanData = (qrCode: string | null) => {
    console.log(qrCode)
    if (qrCode == null) {
      qrCode = "";
    }
    setQrCodeData(qrCode);
    setShowScanner(false);
    setShowQRScannerModal(!showQRScannerModal);
    if (!isClientA) {
      setShowQRCodeModal(!showQRCodeModal);
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
                    <ListItem.Title>{organization.name}</ListItem.Title>
                    <ListItem.Subtitle>ID: {organization.laoId}</ListItem.Subtitle>
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
              value={genQrCodeData}
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
