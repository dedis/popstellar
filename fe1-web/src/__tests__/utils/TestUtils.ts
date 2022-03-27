import testKeyPair from 'test_data/keypair.json';

import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash, KeyPair, PopToken, PublicKey, Timestamp } from 'core/objects';
import { Lao, LaoState } from 'features/lao/objects';

import { EventTypeRollCall, RollCallStatus } from '../../features/rollCall/objects';

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
const mockRCId = new Hash('rollCallId');
const mockRCName = 'myRollCall';
const mockRCLocation = 'location';
const mockRCTimestampStart = new Timestamp(1620255600);
const mockRCTimestampEnd = new Timestamp(1620357600);
const mockRCAttendees = ['attendee1', 'attendee2'];
export const mockRollCallState: any = {
  id: mockRCId.valueOf(),
  eventType: EventTypeRollCall,
  start: mockRCTimestampStart.valueOf(),
  name: mockRCName,
  location: mockRCLocation,
  creation: mockRCTimestampStart.valueOf(),
  proposedStart: mockRCTimestampStart.valueOf(),
  proposedEnd: mockRCTimestampEnd.valueOf(),
  status: RollCallStatus.CLOSED,
  attendees: mockRCAttendees,
};

export const defaultMessageDataFields = ['object', 'action'];

export const mockReduxAction = {
  type: '',
  payload: undefined,
};

export const mockMessageRegistry = new MessageRegistry();
