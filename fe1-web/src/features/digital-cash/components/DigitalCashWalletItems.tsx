import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';

import { PoPIcon } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Hash } from 'core/objects';
import { Color, Icon, List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { DigitalCashFeature } from '../interface';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const DigitalCashWalletItems = ({ laoId }: IPropTypes) => {
  const balance = laoId.valueOf();

  const navigation = useNavigation<NavigationProps['navigation']>();

  // isFirstItem and isLastItem have to be refactored in the future
  const listStyles = List.getListItemStyles(true, false);

  return (
    <>
      <ListItem
        containerStyle={listStyles}
        style={listStyles}
        onPress={() =>
          navigation.navigate(STRINGS.navigation_wallet_digital_cash_wallet, {
            laoId: laoId.valueOf(),
          })
        }>
        <View style={List.icon}>
          <PoPIcon name="digitalCash" color={Color.primary} size={Icon.size} />
        </View>
        <ListItem.Content>
          <ListItem.Title style={Typography.base}>{STRINGS.digital_cash_account}</ListItem.Title>
          <ListItem.Subtitle style={Typography.small}>
            {STRINGS.digital_cash_account_balance}: {balance}
          </ListItem.Subtitle>
        </ListItem.Content>
        <ListItem.Chevron />
      </ListItem>
    </>
  );
};

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
};

DigitalCashWalletItems.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export const digitalCashWalletItemGenerator: DigitalCashFeature.WalletItemGenerator = {
  ListItems: DigitalCashWalletItems as React.ComponentType<{ laoId: Hash }>,
  order: -1000,
};
