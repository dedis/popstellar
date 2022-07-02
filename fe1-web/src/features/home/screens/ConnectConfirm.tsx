import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import { Input, PoPTextButton } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';

/**
 * Ask for confirmation to connect to a specific LAO
 * The ScrollView shows information for the user to verify the authenticity of the LAO
 *
 * TODO Make the confirm button make the action require in the UI specification
 */

type NavigationProps = CompositeScreenProps<
  StackScreenProps<ConnectParamList, typeof STRINGS.navigation_connect_confirm>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

const ConnectConfirm = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();
  const dispatch = useDispatch();

  const laoIdIn = route.params?.laoId || '';
  const url = route.params?.serverUrl || 'ws://localhost:9000/organizer/client';
  const [serverUrl, setServerUrl] = useState(url);
  const [laoId, setLaoId] = useState(laoIdIn);

  const toast = useToast();
  const getLaoChannel = HomeHooks.useGetLaoChannel();
  const getLaoById = HomeHooks.useGetLaoById();
  const resubscribeToLao = HomeHooks.useResubscribeToLao();

  const onButtonConfirm = async () => {
    try {
      const connection = getNetworkManager().connect(serverUrl);

      const laoChannel = getLaoChannel(laoId);
      if (!laoChannel) {
        throw new Error('The given LAO ID is invalid');
      }

      const lao = getLaoById(laoId);

      if (lao) {
        // subscribe to all previously subscribed to channels on the new connection
        await resubscribeToLao(lao, dispatch, [connection]);
      } else {
        // subscribe to the lao channel on the new connection
        await subscribeToChannel(laoId, dispatch, laoChannel, [connection]);
      }

      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_home,
      });
    } catch (err) {
      console.error(`Failed to establish lao connection: ${err}`);
      toast.show(`Failed to establish lao connection: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  return (
    <ScreenWrapper>
      <View style={containerStyles.flex}>
        <Text style={[Typography.paragraph, Typography.important]}>
          {STRINGS.connect_server_uri}
        </Text>
        <Input value={serverUrl} onChange={setServerUrl} placeholder={STRINGS.connect_server_uri} />

        <Text style={[Typography.paragraph, Typography.important]}>{STRINGS.connect_lao_id}</Text>
        <Input value={laoId} onChange={setLaoId} placeholder={STRINGS.connect_lao_id} />

        <PoPTextButton onPress={onButtonConfirm} testID="connect-button">
          {STRINGS.connect_connect}
        </PoPTextButton>
      </View>
    </ScreenWrapper>
  );
};

export default ConnectConfirm;
