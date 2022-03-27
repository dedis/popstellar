import { describe, it } from '@jest/globals';

import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { ProtocolError } from 'core/objects';

import { getWitnessRegistryEntry } from '../WitnessRegistry';

describe('WitnessRegistry', () => {
  it('returns entries for valid message data input', () => {
    getWitnessRegistryEntry({ object: ObjectType.ELECTION, action: ActionType.SETUP });
    getWitnessRegistryEntry({ object: ObjectType.ELECTION, action: ActionType.OPEN });
    getWitnessRegistryEntry({ object: ObjectType.ELECTION, action: ActionType.CAST_VOTE });
    getWitnessRegistryEntry({ object: ObjectType.ELECTION, action: ActionType.END });
  });

  it('throws for invalid message data input', () => {
    expect(() =>
      getWitnessRegistryEntry({ object: ObjectType.CHIRP, action: ActionType.CAST_VOTE }),
    ).toThrow(ProtocolError);
  });
});
