import { mockKeyPair, mockLaoIdHash, mockPopToken } from '__tests__/utils';
import { Timestamp } from 'core/objects';

import { CreateRollCall } from '../network/messages';
import { RollCall, RollCallStatus } from '../objects';

// MOCK ROLL CALL
const mockRollCallName = 'myRollCall';
const mockRollCallLocation = 'location';
const mockRollCallTimestampCreation = new Timestamp(1620255600);
const mockRollCallTimestampStart = new Timestamp(1620255600);
const mockRollCallTimestampEnd = new Timestamp(1620357600);
const mockRollCallAttendees = [mockKeyPair.publicKey, mockPopToken.publicKey];

export const mockRollCall = new RollCall({
  id: CreateRollCall.computeRollCallId(
    mockLaoIdHash,
    mockRollCallTimestampCreation,
    mockRollCallName,
  ),
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

export const mockRollCallState = mockRollCall.toState();

const mockRollCall2Location = 'on pluto';

// same roll id as 'mockRollCall' but a different location
export const mockRollCall2 = new RollCall({
  id: CreateRollCall.computeRollCallId(
    mockLaoIdHash,
    mockRollCallTimestampCreation,
    mockRollCallName,
  ),
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
