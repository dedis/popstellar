import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { Button, QRCode } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { getNetworkManager } from 'core/network';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { selectIsLaoOrganizer, selectIsLaoWitness } from '../reducer';

const styles = StyleSheet.create({
  container: {
    marginTop: Spacing.x1,
    marginBottom: Spacing.x1,
  },
  qrCodeContainer: {
    marginBottom: Spacing.x1,
  },
});

const getUserRole = (isOrganizer: boolean, isWitness: boolean): string => {
  if (isOrganizer) {
    return STRINGS.user_role_organizer;
  }

  if (isWitness) {
    return STRINGS.user_role_witness;
  }

  return STRINGS.user_role_attendee;
};

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const LaoProperties = () => {
  const lao = LaoHooks.useCurrentLao();

  const navigation = useNavigation<NavigationProps['navigation']>();

  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const isWitness = useSelector(selectIsLaoWitness);

  return (
    <View style={styles.container}>
      <View style={styles.qrCodeContainer}>
        <QRCode value={encodeLaoConnection(lao.server_addresses, lao.id.toString())} visibility />
      </View>
      <Text style={Typography.paragraph}>{STRINGS.lao_properties_qr_code_description}</Text>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.lao_properties_id}</Text>
        {'\n'}
        <Text>{lao.id}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>
          {STRINGS.lao_properties_your_role}
        </Text>
        {'\n'}
        <Text>{getUserRole(isOrganizer, isWitness)}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>
          {STRINGS.lao_properties_current_connections}
        </Text>
        {'\n'}
        <Text>{lao.server_addresses.join(', ')}</Text>
      </Text>

      <Button onPress={() => navigation.navigate(STRINGS.navigation_app_connect)}>
        <Text style={[Typography.base, Typography.centered, Typography.negative]}>
          {STRINGS.lao_properties_add_additional_connection}
        </Text>
      </Button>

      <Button
        onPress={() => {
          getNetworkManager().disconnectFromAll();
          navigation.navigate(STRINGS.navigation_app_home, {
            screen: STRINGS.navigation_home_home,
          });
        }}>
        <Text style={[Typography.base, Typography.centered, Typography.negative]}>
          {STRINGS.lao_properties_disconnect}
        </Text>
      </Button>
    </View>
  );
};

export default LaoProperties;
