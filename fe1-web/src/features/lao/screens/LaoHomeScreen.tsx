import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Modal, StyleSheet, Text, View, ViewStyle } from 'react-native';
import {
  ScrollView,
  TouchableOpacity,
  TouchableWithoutFeedback,
} from 'react-native-gesture-handler';

import DropdownIcon from 'core/components/icons/DropdownIcon';
import NotificationIcon from 'core/components/icons/NotificationIcon';
import ModalHeader from 'core/components/ModalHeader';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Icon, ModalStyles, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoProperties } from '../components';
import Identity from '../components/Identity';
import { LaoHooks } from '../hooks';

const LaoHomeScreen = () => {
  return (
    <ScreenWrapper>
      <Identity />
    </ScreenWrapper>
  );
};

export default LaoHomeScreen;

const styles = StyleSheet.create({
  iconView: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
  icon: {
    marginLeft: Spacing.x1,
  } as ViewStyle,
  header: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
  dropdownIcon: {
    marginLeft: Spacing.x025,
  },
});

export const LaoHomeScreenHeader = () => {
  const lao = LaoHooks.useCurrentLao();
  const [modalVisible, setModalVisible] = useState(false);

  return (
    <>
      <TouchableOpacity onPress={() => setModalVisible(!modalVisible)} style={styles.header}>
        <Text style={Typography.topNavigationHeading}>{lao.name}</Text>
        <View style={styles.dropdownIcon}>
          <DropdownIcon color={Color.primary} size={Icon.size} />
        </View>
      </TouchableOpacity>

      <Modal
        transparent
        presentationStyle="formSheet"
        visible={modalVisible}
        onRequestClose={() => {
          setModalVisible(!modalVisible);
        }}>
        <TouchableWithoutFeedback
          containerStyle={ModalStyles.modalBackground}
          onPress={() => {
            setModalVisible(!modalVisible);
          }}
        />
        <ScrollView style={ModalStyles.modalContainer}>
          <ModalHeader onClose={() => setModalVisible(!modalVisible)}>{lao.name}</ModalHeader>
          <LaoProperties />
        </ScrollView>
      </Modal>
    </>
  );
};

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

export const LaoHomeScreenHeaderRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  return (
    <View style={styles.iconView}>
      <TouchableOpacity
        style={styles.icon}
        onPress={() =>
          navigation.push(STRINGS.navigation_app_lao, {
            screen: STRINGS.navigation_lao_notifications,
            params: {
              screen: STRINGS.navigation_notification_notifications,
            },
          })
        }>
        <NotificationIcon color={Color.inactive} size={Icon.size} />
      </TouchableOpacity>
    </View>
  );
};
