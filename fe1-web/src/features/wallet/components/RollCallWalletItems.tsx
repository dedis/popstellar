import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';

import { PoPIcon } from 'core/components';
import { Hash } from 'core/objects';
import { Color, Icon, List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletHooks } from '../hooks';
import { WalletFeature } from '../interface';
import RollCallWalletItem from './RollCallWalletItem';

const RollCallWalletItems = ({ laoId }: IPropTypes) => {
  const tokens = WalletHooks.useRollCallTokensByLaoId(laoId.valueOf());

  console.log(tokens);

  if (tokens.length > 0) {
    return (
      <>
        {tokens.map((token, idx) => (
          // isFirstItem and isLastItem have to be refactored in the future
          // since it is not known what other items other features add
          <RollCallWalletItem
            key={token.rollCallId.valueOf()}
            rollCallToken={token}
            isFirstItem={false}
            isLastItem={idx === tokens.length - 1}
          />
        ))}
      </>
    );
  }

  const listStyles = List.getListItemStyles(false, true);

  return (
    <ListItem containerStyle={listStyles} style={listStyles}>
      <View style={List.icon}>
        <PoPIcon name="qrCode" color={Color.primary} size={Icon.size} />
      </View>
      <ListItem.Content>
        <ListItem.Title style={Typography.base}>
          {STRINGS.wallet_home_rollcall_no_pop_tokens}
        </ListItem.Title>
        <ListItem.Subtitle style={Typography.small}>
          {STRINGS.wallet_home_rollcall_no_pop_tokens_description}
        </ListItem.Subtitle>
      </ListItem.Content>
    </ListItem>
  );
};

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
};

RollCallWalletItems.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RollCallWalletItems;

export const rollCallWalletItemGenerator: WalletFeature.WalletItemGenerator = {
  ListItems: RollCallWalletItems as React.ComponentType<{ laoId: Hash }>,
  order: 1000,
};
