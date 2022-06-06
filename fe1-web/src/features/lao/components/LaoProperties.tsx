import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import { PoPTextButton } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { getNetworkManager } from 'core/network';
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

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const LaoProperties = () => {
  const lao = LaoHooks.useCurrentLao();

  const navigation = useNavigation<NavigationProps['navigation']>();

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const isWitness = useSelector(selectIsLaoWitness);

  return (
    <View>
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

      <PoPTextButton onPress={() => navigation.navigate(STRINGS.navigation_app_connect)}>
        {STRINGS.lao_properties_add_additional_connection}
      </PoPTextButton>

      <PoPTextButton
        onPress={() => {
          getNetworkManager().disconnectFromAll();
          navigation.navigate(STRINGS.navigation_app_home, {
            screen: STRINGS.navigation_home_home,
          });
        }}>
        {STRINGS.lao_properties_disconnect}
      </PoPTextButton>
    </View>
  );
};

export default LaoProperties;
