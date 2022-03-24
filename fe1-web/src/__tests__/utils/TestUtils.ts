import testKeyPair from 'test_data/keypair.json';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, KeyPair, PopToken, PublicKey, Timestamp } from 'core/objects';
import { Lao, LaoState } from 'features/lao/objects';

import { EventTypeRollCall, RollCall, RollCallStatus } from '../../features/rollCall/objects';

export const mockPublicKey = testKeyPair.publicKey;
export const mockPrivateKey = testKeyPair.privateKey;
export const mockKeyPair = KeyPair.fromState({
  publicKey: mockPublicKey,
  privateKey: mockPrivateKey,
});

export const mockPublicKey2 = testKeyPair.publicKey2;
export const mockPrivateKey2 = testKeyPair.privateKey2;

export const mockPopToken = PopToken.fromState({
  publicKey: testKeyPair.publicKey2,
  privateKey: testKeyPair.privateKey2,
});

export const org = new PublicKey(mockPublicKey);

// MOCK LAO
export const mockLaoName = 'MyLao';
export const mockLaoCreationTime = new Timestamp(1600000000);
export const mockLaoIdHash: Hash = Hash.fromStringArray(
  org.toString(),
  mockLaoCreationTime.toString(),
  mockLaoName,
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

// MOCK ROLL CALL
export const mockId = new Hash('rollCallId');
export const mockName = 'myRollCall';
export const mockLocation = 'location';
export const mockTimestampStart = new Timestamp(1620255600);
export const mockTimestampEnd = new Timestamp(1620357600);
export const mockAttendees = ['attendee1', 'attendee2'];
export const mockRollCallState: any = {
  id: mockId.valueOf(),
  eventType: EventTypeRollCall,
  start: mockTimestampStart.valueOf(),
  name: mockName,
  location: mockLocation,
  creation: mockTimestampStart.valueOf(),
  proposedStart: mockTimestampStart.valueOf(),
  proposedEnd: mockTimestampEnd.valueOf(),
  status: RollCallStatus.CLOSED,
  attendees: mockAttendees,
};

export const mockRollCall = RollCall.fromState(mockRollCallState);

export const defaultMessageDataFields = ['object', 'action'];

export const mockReduxAction = {
  type: '',
  payload: undefined,
};

export const mockMessageRegistry = new MessageRegistry();
