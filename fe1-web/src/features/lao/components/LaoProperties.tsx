import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { QRCode } from 'core/components';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { selectIsLaoOrganizer, selectIsLaoWitness } from '../reducer';

const getUserRole = (isOrganizer: boolean, isWitness: boolean): string => {
  if (isOrganizer) {
    return STRINGS.user_role_organizer;
  }

  if (isWitness) {
    return STRINGS.user_role_witness;
  }

  return STRINGS.user_role_attendee;
};

const styles = StyleSheet.create({
  qrcodeContainer: {
    marginVertical: Spacing.x05,
  },
});

const LaoProperties = () => {
  const lao = LaoHooks.useCurrentLao();
  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const isWitness = useSelector(selectIsLaoWitness);

  return (
    <View>
      <View style={styles.qrcodeContainer}>
        <QRCode
          value={encodeLaoConnection(lao.server_addresses, lao.id)}
          overlayText={STRINGS.lao_qr_code_overlay}
        />
      </View>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.lao_properties_name}</Text>
        {'\n'}
        <Text selectable>{lao.name}</Text>
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
        <Text selectable>{lao.server_addresses.join(', ')}</Text>
      </Text>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.lao_properties_id}</Text>
        {'\n'}
        <Text selectable>{lao.id}</Text>
      </Text>
    </View>
  );
};

export default LaoProperties;
