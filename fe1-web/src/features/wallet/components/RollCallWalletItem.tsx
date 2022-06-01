import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';

import { PoPIcon } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Color, Icon, List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { RollCallToken } from '../objects/RollCallToken';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const RollCallWalletItem = ({ rollCallToken, isFirstItem, isLastItem }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const listStyles = List.getListItemStyles(isFirstItem, isLastItem);

  return (
    <ListItem
      key={rollCallToken.rollCallId.valueOf()}
      containerStyle={listStyles}
      style={listStyles}
      onPress={() =>
        navigation.navigate(STRINGS.navigation_wallet_single_roll_call, {
          rollCallId: rollCallToken.rollCallId.valueOf(),
          rollCallName: rollCallToken.rollCallName,
          rollCallTokenPublicKey: rollCallToken.token.publicKey.valueOf(),
        })
      }>
      <View style={List.icon}>
        <PoPIcon name="qrCode" color={Color.primary} size={Icon.size} />
      </View>
      <ListItem.Content>
        <ListItem.Title style={Typography.base}>{rollCallToken.rollCallName}</ListItem.Title>
        <ListItem.Subtitle style={Typography.small}>
          {STRINGS.wallet_home_rollcall_pop_token}
        </ListItem.Subtitle>
      </ListItem.Content>
      <ListItem.Chevron />
    </ListItem>
  );
};

const propTypes = {
  rollCallToken: PropTypes.instanceOf(RollCallToken).isRequired,
  isFirstItem: PropTypes.bool.isRequired,
  isLastItem: PropTypes.bool.isRequired,
};

RollCallWalletItem.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallWalletItem;
