import 'jest-extended';

import '__tests__/utils/matchers';
import { ActionType, ObjectType } from 'core/network/messages/MessageData';
import { Hash, Timestamp, ProtocolError } from 'core/objects';

import { AddChirp } from '../AddChirp';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const TEXT = 'text';
const PARENT_ID = new Hash('parentId');

const sampleAddChirp: Partial<AddChirp> = {
  object: ObjectType.CHIRP,
  action: ActionType.ADD,
  text: TEXT,
  timestamp: TIMESTAMP,
  parent_id: PARENT_ID,
};

const addChirpJson = `{
  "object": "${ObjectType.CHIRP}",
  "action": "${ActionType.ADD}",
  "text": "${TEXT}",
  "timestamp": ${TIMESTAMP},
  "parent_id": "${PARENT_ID}"
}`;

describe('AddChirp', () => {
  it('should be created correctly from Json', () => {
    expect(new AddChirp(sampleAddChirp)).toBeJsonEqual(sampleAddChirp);
    const temp = {
      object: ObjectType.CHIRP,
      action: ActionType.ADD,
      text: TEXT,
      timestamp: TIMESTAMP,
    };
    expect(new AddChirp(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from JSON', () => {
    const obj = JSON.parse(addChirpJson);
    expect(AddChirp.fromJson(obj)).toBeJsonEqual(sampleAddChirp);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.END,
      text: TEXT,
      timestamp: TIMESTAMP,
      parent_id: PARENT_ID,
    };
    const createFromJson = () => AddChirp.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if text is undefined', () => {
      const createWrongObj = () =>
        new AddChirp({
          object: ObjectType.CHIRP,
          action: ActionType.CREATE,
          timestamp: TIMESTAMP,
          parent_id: PARENT_ID,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if text is too long', () => {
      const bigMessage =
        "It seems that this messages won't fit. It seems that this messages won't fit. " +
        "It seems that this messages won't fit. It seems that this messages won't fit. It seems that " +
        "this messages won't fit. It seems that this messages won't fit. It seems that this messages " +
        "won't fit. It seems that this messages won't fit.";
      const createWrongObj = () =>
        new AddChirp({
          object: ObjectType.CHIRP,
          action: ActionType.CREATE,
          text: bigMessage,
          timestamp: TIMESTAMP,
          parent_id: PARENT_ID,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if timestamp is undefined', () => {
      const createWrongObj = () =>
        new AddChirp({
          object: ObjectType.CHIRP,
          action: ActionType.CREATE,
          text: TEXT,
          parent_id: PARENT_ID,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
