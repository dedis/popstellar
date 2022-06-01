import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';

import { Hash } from 'core/objects';

import { WalletHooks } from '../hooks';
import { WalletFeature } from '../interface';
import { recoverWalletRollCallTokens } from '../objects';
import { RollCallToken } from '../objects/RollCallToken';
import RollCallWalletItem from './RollCallWalletItem';

const RollCallWalletItems = ({ laoId }: IPropTypes) => {
  const rollCallsById = WalletHooks.useRollCallsByLaoId(laoId.valueOf());

  const [tokens, setTokens] = useState<RollCallToken[]>([]);

  useEffect(() => {
    let updateWasCanceled = false;

    recoverWalletRollCallTokens(Object.values(rollCallsById), laoId).then((value) => {
      // then update the state if no new update was triggered
      if (!updateWasCanceled) {
        setTokens(value);
      }
    });

    return () => {
      // cancel update if the hook is called again
      updateWasCanceled = true;
    };
  }, [rollCallsById, laoId]);

  return (
    <>
      {tokens.map((token, idx) => (
        <RollCallWalletItem
          key={token.rollCallId.valueOf()}
          rollCallToken={token}
          isFirstItem={idx === 0}
          isLastItem={idx === tokens.length - 1}
        />
      ))}
    </>
  );
};

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
};

RollCallWalletItems.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export const rollCallWalletItemGenerator: WalletFeature.WalletItemGenerator = {
  ListItems: RollCallWalletItems as React.ComponentType<{ laoId: Hash }>,
  order: 1000,
};
