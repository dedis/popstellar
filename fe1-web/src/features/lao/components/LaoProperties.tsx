import React from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { Typography } from 'core/styles';
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

const LaoProperties = () => {
  const lao = LaoHooks.useCurrentLao();

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const isWitness = useSelector(selectIsLaoWitness);

  return (
    <View>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.lao_properties_id}</Text>
        {'\n'}
        <Text selectable>{lao.id}</Text>
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
    </View>
  );
};

export default LaoProperties;
