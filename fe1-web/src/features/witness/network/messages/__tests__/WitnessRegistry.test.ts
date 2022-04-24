import { describe, it } from '@jest/globals';

import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { ProtocolError } from 'core/objects';

import { getWitnessRegistryEntryType } from '../WitnessRegistry';

describe('WitnessRegistry', () => {
  it('does not throw an error for valid message data input', () => {
    expect(() => {
      getWitnessRegistryEntryType({ object: ObjectType.ELECTION, action: ActionType.SETUP });
      getWitnessRegistryEntryType({ object: ObjectType.ELECTION, action: ActionType.OPEN });
      getWitnessRegistryEntryType({ object: ObjectType.ELECTION, action: ActionType.CAST_VOTE });
      getWitnessRegistryEntryType({ object: ObjectType.ELECTION, action: ActionType.END });
    }).not.toThrow(ProtocolError);
  });

  it('throws for invalid message data input', () => {
    expect(() =>
      getWitnessRegistryEntryType({ object: ObjectType.CHIRP, action: ActionType.CAST_VOTE }),
    ).toThrow(ProtocolError);
  });
});
