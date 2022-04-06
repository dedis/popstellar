import testKeyPair from 'test_data/keypair.json';

import { EventTags, Hash, PublicKey, Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';
import { addEvent, removeEvent } from 'features/events/reducer';
import { connectToLao, disconnectFromLao, removeLao } from 'features/lao/reducer';
import { EventTypeRollCall, RollCall, RollCallStatus } from 'features/rollCall/objects';

import { Lao, LaoState } from '../../lao/objects';
import { generateToken } from './Token';

// region Dummy values definition
// MOCK LAO
export const mockPublicKey = testKeyPair.publicKey;

export const org = new PublicKey(mockPublicKey);

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
  server_addresses: [],
};
export const mockLao = Lao.fromState(mockLaoState);

// MOCK ROLL CALL
const mockRCName = 'myRollCall';
const mockRCLocation = 'location';
const mockRCTimestampStart = new Timestamp(1620255600);
const mockRCTimestampEnd = new Timestamp(1620357600);
const mockRCAttendees = ['attendee1', 'attendee2'];

const mockRCIdHash = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLaoId,
  mockRCTimestampStart.toString(),
  mockRCName,
);

export const mockRollCallState: any = {
  id: mockRCIdHash.valueOf(),
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
// endregion

const createRollCall = (id: string, name: string, mockAttendees: string[]) => {
  return RollCall.fromState({
    ...mockRollCallState,
    id: id,
    name: name,
    attendees: [...mockRollCallState.attendees, ...mockAttendees],
  });
};

const mockRCName0: string = 'mock0';
const mockRCName1: string = 'mock1';

const hashMock0 = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLao.id.valueOf(),
  mockRollCallState.start.valueOf(),
  mockRCName0,
);
const hashMock1 = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLao.id.valueOf(),
  mockRollCallState.start.valueOf(),
  mockRCName1,
);
/*
 * Generates a mock state with some mock popTokens
 */
export async function createDummyWalletState() {
  const tokenMockRC0 = await generateToken(mockLao.id, hashMock0);
  const tokenMockRC1 = await generateToken(mockLao.id, hashMock1);
  const mockRollCall0 = createRollCall(hashMock0.valueOf(), mockRCName0, [
    tokenMockRC0.publicKey.valueOf(),
  ]);
  const mockRollCall1 = createRollCall(hashMock1.valueOf(), mockRCName1, [
    tokenMockRC1.publicKey.valueOf(),
  ]);
  dispatch(connectToLao(mockLao.toState()));
  dispatch(addEvent(mockLao.id, mockRollCall0.toState()));
  dispatch(addEvent(mockLao.id, mockRollCall1.toState()));
  console.debug('Dispatched mock events');
}
/*
 * Clears the mock state
 */
export function clearDummyWalletState() {
  dispatch(removeEvent(mockLao.id, hashMock0));
  dispatch(removeEvent(mockLao.id, hashMock1));
  dispatch(disconnectFromLao());
  dispatch(removeLao(mockLao.id));
}
