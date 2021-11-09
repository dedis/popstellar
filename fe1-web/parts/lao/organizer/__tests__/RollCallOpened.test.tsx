import React from 'react';
import { render } from '@testing-library/react-native';
import {
  Base64UrlData, Hash, PrivateKey, PublicKey, Timestamp,
} from 'model/objects';
import keyPair from 'test_data/keypair.json';
import RollCallOpened from '../RollCallOpened';

export const mockPublicKey = new PublicKey(keyPair.publicKey);
export const mockSecretKey = new PrivateKey(keyPair.privateKey);

const org = mockPublicKey;
const time = new Timestamp(1609455600); // 1st january 2021
const name = 'lao for testng roll-call';
const location = 'EPFL';
const mockLaoId: Hash = Hash.fromStringArray(org.toString(), time.toString(), name);
const rollCallId = Hash.fromStringArray('R', mockLaoId.toString(), time.toString(), name.toString());

describe('RollCallOpened', () => {
  it('renders correctly when no scan', () => {
    const { toJSON } = render(
      <RollCallOpened rollCallID={rollCallId} time={time} />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
