import React, { useEffect, useMemo, useState } from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';

import { rollCallWalletItemGenerator } from '../components/RollCallWalletItems';
import { WalletHooks } from '../hooks';

/**
 * Wallet UI once the wallet is synced
 */
const WalletHome = () => {
  const namesByLaoId = WalletHooks.useNamesByLaoId();
  const currentLaoId = WalletHooks.useCurrentLaoId();
  const knownLaoIds = WalletHooks.useLaoIds();

  const generators = WalletHooks.useWalletItemGenerators();

  const [isLaoExpanded, setIsLaoExpanded] = useState<Record<string, boolean | undefined>>({});

  const walletItemGenerators = useMemo(() => {
    return [...generators, rollCallWalletItemGenerator].sort((a, b) => a.order - b.order);
  }, [generators]);

  useEffect(() => {
    if (currentLaoId) {
      setIsLaoExpanded((oldValue) => ({
        ...oldValue,
        [currentLaoId.valueOf()]: true,
      }));
    }
  }, [currentLaoId]);

  // if we are connected to a lao, then only show data for this lao
  const laoIds = currentLaoId ? [currentLaoId] : knownLaoIds;

  return (
    <ScreenWrapper>
      <View style={List.container}>
        {laoIds.map((laoIdHash) => {
          const laoId = laoIdHash.valueOf();

          return (
            <ListItem.Accordion
              key={laoId}
              containerStyle={List.accordionItem}
              style={List.accordionItem}
              content={
                <ListItem.Content>
                  <ListItem.Title style={[Typography.base, Typography.important]}>
                    {namesByLaoId[laoId]}
                  </ListItem.Title>
                </ListItem.Content>
              }
              isExpanded={!!isLaoExpanded[laoId]}
              onPress={() =>
                setIsLaoExpanded({
                  ...isLaoExpanded,
                  [laoId]: !isLaoExpanded[laoId],
                })
              }>
              {walletItemGenerators.map((Generator, idx) => (
                // FIXME: Do not use index in key
                // eslint-disable-next-line react/no-array-index-key
                <Generator.ListItems key={idx.toString()} laoId={laoIdHash} />
              ))}
            </ListItem.Accordion>
          );
        })}
      </View>
    </ScreenWrapper>
  );
};

export default WalletHome;
