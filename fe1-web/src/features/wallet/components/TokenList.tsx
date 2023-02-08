import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { PoPIcon } from 'core/components';
import { Hash } from 'core/objects';
import { Color, Icon, List, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { WalletHooks } from '../hooks';
import Token from './Token';

const styles = StyleSheet.create({
  validToken: {
    paddingBottom: Spacing.x1,
  },
  title: {
    paddingBottom: Spacing.x05,
  },
});

const TokenList = ({ laoId }: IPropTypes) => {
  const tokens = WalletHooks.useRollCallTokensByLaoId(laoId);
  const lastTokenizedRollCallId = WalletHooks.useCurrentLao().last_tokenized_roll_call_id;

  const lastTokenizedRollCall = useMemo(
    () =>
      lastTokenizedRollCallId
        ? tokens.find((t) => t.rollCallId.equals(lastTokenizedRollCallId))
        : undefined,
    [tokens, lastTokenizedRollCallId],
  );

  const previousTokens = useMemo(
    () =>
      tokens.filter(
        (t) => !lastTokenizedRollCallId || !t.rollCallId.equals(lastTokenizedRollCallId),
      ),
    [tokens, lastTokenizedRollCallId],
  );

  if (tokens.length > 0) {
    return (
      <View>
        {lastTokenizedRollCall && (
          <View style={styles.validToken}>
            <Text style={[Typography.base, Typography.important, styles.title]}>
              {STRINGS.wallet_home_rollcall_current_pop_tokens}
            </Text>
            <Token
              rollCallToken={lastTokenizedRollCall}
              subtitle={STRINGS.wallet_home_rollcall_pop_token_valid}
              isFirstItem
              isLastItem
            />
          </View>
        )}
        {previousTokens.length > 0 && (
          <View>
            <Text style={[Typography.base, Typography.important, styles.title]}>
              {STRINGS.wallet_home_rollcall_previous_pop_tokens}
            </Text>
            {previousTokens.map((token, idx) => (
              // isFirstItem and isLastItem have to be refactored in the future
              // since it is not known what other items other features add
              <Token
                key={token.rollCallId.valueOf()}
                rollCallToken={token}
                isFirstItem={idx === 0}
                isLastItem={idx === previousTokens.length - 1}
              />
            ))}
          </View>
        )}
      </View>
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

TokenList.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TokenList;
