import 'jest-extended';
import '__tests__/utils/matchers';

import { Base64UrlData, Hash, Timestamp, ProtocolError } from 'core/objects';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages/MessageData';

import { DeleteChirp } from '../DeleteChirp';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const mockMessageId = Base64UrlData.encode('message_id');
const ID = new Hash(mockMessageId.toString());

const sampleDeleteChirp: Partial<DeleteChirp> = {
  object: ObjectType.CHIRP,
  action: ActionType.DELETE,
  chirp_id: ID,
  timestamp: TIMESTAMP,
};

const deleteChirp = `{
  "object": "${ObjectType.CHIRP}",
  "action": "${ActionType.DELETE}",
  "chirp_id": "${ID}",
  "timestamp": ${TIMESTAMP}
  }`;

describe('DeleteChirp', () => {
  it('should be created correctly from JSON', () => {
    expect(new DeleteChirp(sampleDeleteChirp)).toBeJsonEqual(sampleDeleteChirp);
    const temp = {
      object: ObjectType.CHIRP,
      action: ActionType.DELETE,
      chirp_id: ID,
      timestamp: TIMESTAMP,
    };
    expect(new DeleteChirp(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from JSON', () => {
    const obj = JSON.parse(deleteChirp);
    expect(DeleteChirp.fromJson(obj)).toBeJsonEqual(sampleDeleteChirp);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.CREATE,
      chirp_id: ID,
      timestamp: TIMESTAMP,
    };
    const createFromJson = () => DeleteChirp.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if id is undefined', () => {
      const wrongObj = () =>
        new DeleteChirp({
          object: ObjectType.CHIRP,
          action: ActionType.DELETE,
          timestamp: TIMESTAMP,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if timestamp is undefined', () => {
      const wrongObj = () =>
        new DeleteChirp({
          object: ObjectType.CHIRP,
          action: ActionType.DELETE,
          chirp_id: ID,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });
  });
});
