import { mockKeyPair, mockLaoId, mockPopToken } from '__tests__/utils';
import { Hash, PublicKey, Timestamp } from 'core/objects';

import { CreateRollCall, OpenRollCall } from '../network/messages';
import { RollCall, RollCallState, RollCallStatus } from '../objects';

// MOCK ROLL CALL
const mockRollCallName = 'myRollCall';
const mockRollCallLocation = 'location';
const mockRollCallTimestampCreation = new Timestamp(1620355600);
const mockRollCallTimestampStart = new Timestamp(1620355600);
const mockRollCallTimestampEnd = new Timestamp(1620357600);
const mockRollCallAttendees = [mockKeyPair.publicKey, mockPopToken.publicKey];

export const mockRollCall = new RollCall({
  id: CreateRollCall.computeRollCallId(mockLaoId, mockRollCallTimestampCreation, mockRollCallName),
  start: mockRollCallTimestampStart,
  end: mockRollCallTimestampEnd,
  name: mockRollCallName,
  location: mockRollCallLocation,
  creation: mockRollCallTimestampCreation,
  proposedStart: mockRollCallTimestampStart,
  proposedEnd: mockRollCallTimestampEnd,
  status: RollCallStatus.CREATED,
  attendees: mockRollCallAttendees,
});

export const mockRollCallState = mockRollCall.toState() as RollCallState & {
  attendees: PublicKey[];
};

const mockRollCall2Location = 'on pluto';

// same roll id as 'mockRollCall' but a different location
export const mockRollCall2 = new RollCall({
  id: CreateRollCall.computeRollCallId(mockLaoId, mockRollCallTimestampCreation, mockRollCallName),
  start: mockRollCallTimestampStart,
  end: mockRollCallTimestampEnd,
  name: mockRollCallName,
  location: mockRollCall2Location,
  creation: mockRollCallTimestampCreation,
  proposedStart: mockRollCallTimestampStart,
  proposedEnd: mockRollCallTimestampEnd,
  status: RollCallStatus.CREATED,
  attendees: mockRollCallAttendees,
});

export const mockRollCallState2 = mockRollCall2.toState();

const mockRollCallWithAliasOpenedAt = new Timestamp(1620357600);

/**
 * An "updated" roll call where 'mockRollCall' is the roll call that was updated
 */
export const mockRollCallWithAlias = new RollCall({
  id: mockRollCall.id,
  idAlias: OpenRollCall.computeOpenRollCallId(
    mockLaoId,
    mockRollCall.id,
    mockRollCallWithAliasOpenedAt,
  ),
  start: mockRollCallTimestampStart,
  end: mockRollCallTimestampEnd,
  name: mockRollCallName,
  location: mockRollCallLocation,
  creation: mockRollCallTimestampCreation,
  proposedStart: mockRollCallTimestampStart,
  proposedEnd: mockRollCallTimestampEnd,
  status: RollCallStatus.OPENED,
  attendees: mockRollCallAttendees,
  openedAt: mockRollCallWithAliasOpenedAt,
}) as RollCall & { idAlias: Hash };

export const mockRollCallWithAliasState = mockRollCallWithAlias.toState() as RollCallState & {
  idAlias: string;
};
