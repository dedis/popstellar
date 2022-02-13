import {
  Hash, Lao, LaoState, PopToken, PublicKey, Timestamp,
} from 'model/objects';
import testKeyPair from 'test_data/keypair.json';

export const mockPublicKey = testKeyPair.publicKey;
export const mockPrivateKey = testKeyPair.privateKey;
export const mockPublicKey2 = testKeyPair.publicKey2;
export const mockPopToken = PopToken.fromState({
  publicKey: testKeyPair.publicKey2,
  privateKey: testKeyPair.privateKey2,
});

export const org = new PublicKey(mockPublicKey);
export const mockLaoName = 'MyLao';
export const mockLaoCreationTime = new Timestamp(1600000000);
export const mockLaoIdHash: Hash = Hash.fromStringArray(
  org.toString(), mockLaoCreationTime.toString(), mockLaoName,
);
export const mockLaoId: string = mockLaoIdHash.toString();

export const mockLaoState: LaoState = {
  id: mockLaoId,
  name: mockLaoName,
  creation: mockLaoCreationTime.valueOf(),
  last_modified: mockLaoCreationTime.valueOf(),
  organizer: org.valueOf(),
  witnesses: [],
};
export const mockLao = Lao.fromState(mockLaoState);
