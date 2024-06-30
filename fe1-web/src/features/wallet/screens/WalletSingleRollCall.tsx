import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import * as Clipboard from 'expo-clipboard';
import React from 'react';
import { StyleSheet, Text, TextStyle, View } from 'react-native';

import { PoPTextButton, QRCode } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { generateUsernameFromBase64 } from 'core/functions/Mnemonic';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { ScannablePopToken } from 'core/objects/ScannablePopToken';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_single_roll_call>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const styles = StyleSheet.create({
  publicKey: {
    marginTop: Spacing.x05,
    marginBottom: Spacing.x1,
    color: Color.inactive,
    textAlign: 'center',
  } as TextStyle,
  username: {
    marginTop: Spacing.x2,
  } as TextStyle,
});

/**
 * Renders the screen for showing a single roll call in the wallet
 */
const WalletSingleRollCall = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallTokenPublicKey } = route.params;

  return (
    <ScreenWrapper>
      <View>
        <QRCode
          value={ScannablePopToken.encodePopToken({ pop_token: rollCallTokenPublicKey })}
          overlayText={STRINGS.wallet_home_rollcall_qrcode_text}
        />
      </View>

      <Text style={[Typography.important, Typography.centered, Typography.code, styles.username]}>
        {generateUsernameFromBase64(rollCallTokenPublicKey)}
      </Text>
      <Text style={[Typography.small, styles.publicKey, Typography.code]} selectable>
        {rollCallTokenPublicKey}
      </Text>

      <PoPTextButton onPress={() => Clipboard.setStringAsync(rollCallTokenPublicKey)}>
        {STRINGS.general_copy}
      </PoPTextButton>
    </ScreenWrapper>
  );
};

export default WalletSingleRollCall;

/**
 * Component shown in the top middle of the navigation bar. Makes sure
 * the name of a roll call is shown instead of some static text
 */
export const ViewSingleRollCallScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { rollCallName } = route.params;

  return (
    <Text style={Typography.topNavigationHeading} numberOfLines={1}>
      {rollCallName}
    </Text>
  );
};
