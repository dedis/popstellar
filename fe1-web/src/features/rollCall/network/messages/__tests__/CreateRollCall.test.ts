import 'jest-extended';
import '__tests__/utils/matchers';

import { configureTestFeatures, mockLaoId } from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, Timestamp } from 'core/objects';

import { CreateRollCall } from '../CreateRollCall';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const TIMESTAMP_BEFORE = new Timestamp(1609445600);
const NAME = 'myRollCall';
const LOCATION = 'location';
const DESCRIPTION = 'Roll Call description';
const rollCallId = Hash.fromArray('R', mockLaoId, TIMESTAMP, NAME);

const sampleCreateRollCall: Partial<CreateRollCall> = {
  object: ObjectType.ROLL_CALL,
  action: ActionType.CREATE,
  id: rollCallId,
  name: NAME,
  creation: TIMESTAMP,
  proposed_start: TIMESTAMP,
  proposed_end: CLOSE_TIMESTAMP,
  location: LOCATION,
  description: DESCRIPTION,
};

const createRollCallJson = `{
  "object": "${ObjectType.ROLL_CALL}",
  "action": "${ActionType.CREATE}",
  "id": "${rollCallId}",
  "name": "${NAME}",
  "creation": ${TIMESTAMP},
  "proposed_start": ${TIMESTAMP},
  "proposed_end": ${CLOSE_TIMESTAMP},
  "location": "${LOCATION}",
  "description": "${DESCRIPTION}"
}`;

beforeAll(() => {
  configureTestFeatures();
});

describe('CreateRollCall', () => {
  it('should be created correctly from Json', () => {
    expect(new CreateRollCall(sampleCreateRollCall, mockLaoId)).toBeJsonEqual(sampleCreateRollCall);
    let temp: any = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      id: rollCallId,
      name: NAME,
      creation: TIMESTAMP,
      proposed_start: TIMESTAMP,
      proposed_end: CLOSE_TIMESTAMP,
      location: LOCATION,
      description: DESCRIPTION,
    };
    expect(new CreateRollCall(temp, mockLaoId)).toBeJsonEqual(temp);

    temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      id: rollCallId,
      name: NAME,
      creation: TIMESTAMP,
      proposed_start: TIMESTAMP,
      proposed_end: CLOSE_TIMESTAMP,
      location: LOCATION,
    };
    expect(new CreateRollCall(temp, mockLaoId)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(createRollCallJson);
    expect(CreateRollCall.fromJson(obj, mockLaoId)).toBeJsonEqual(sampleCreateRollCall);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.ADD,
      id: rollCallId,
      name: NAME,
      creation: TIMESTAMP,
      proposed_start: TIMESTAMP,
      proposed_end: CLOSE_TIMESTAMP,
      location: LOCATION,
      description: DESCRIPTION,
    };
    const createWrongObj = () => CreateRollCall.fromJson(obj, mockLaoId);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if id is undefined', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            name: NAME,
            creation: TIMESTAMP,
            proposed_start: TIMESTAMP,
            proposed_end: CLOSE_TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if name is undefined', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            creation: TIMESTAMP,
            proposed_start: TIMESTAMP,
            proposed_end: CLOSE_TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if create is undefined', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            name: NAME,
            proposed_start: TIMESTAMP,
            proposed_end: CLOSE_TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if proposed_start is undefined', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            name: NAME,
            creation: TIMESTAMP,
            proposed_end: CLOSE_TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if proposed_end is undefined', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            name: NAME,
            creation: TIMESTAMP,
            proposed_start: TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if location is undefined', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            name: NAME,
            creation: TIMESTAMP,
            proposed_start: TIMESTAMP,
            proposed_end: CLOSE_TIMESTAMP,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is incorrect', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: new Hash('id'),
            name: NAME,
            creation: TIMESTAMP,
            proposed_start: TIMESTAMP,
            proposed_end: CLOSE_TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if proposed_start is before creation', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            name: NAME,
            creation: TIMESTAMP,
            proposed_start: TIMESTAMP_BEFORE,
            proposed_end: CLOSE_TIMESTAMP,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if proposed_end is before proposed_start', () => {
      const createWrongObj = () =>
        new CreateRollCall(
          {
            object: ObjectType.ROLL_CALL,
            action: ActionType.CREATE,
            id: rollCallId,
            name: NAME,
            creation: TIMESTAMP_BEFORE,
            proposed_start: TIMESTAMP,
            proposed_end: TIMESTAMP_BEFORE,
            location: LOCATION,
            description: DESCRIPTION,
          },
          mockLaoId,
        );
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
