import 'jest-extended';
import '__tests__/utils/matchers';

import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Base64UrlData, Hash, ProtocolError, Timestamp } from 'core/objects';

import { DeleteReaction } from '../DeleteReaction';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const mockReactionId = Base64UrlData.encode('reaction_id');
const ID = new Hash(mockReactionId.toString());

const sampleDeleteReaction = {
  object: ObjectType.REACTION,
  action: ActionType.DELETE,
  reaction_id: ID,
  timestamp: TIMESTAMP,
};

const dataDeleteReaction = `{
  "object": "${ObjectType.REACTION}",
  "action": "${ActionType.DELETE}",
  "reaction_id": "${ID.toState()}",
  "timestamp": ${TIMESTAMP.toState()}
  }`;

describe('DeleteReaction', () => {
  it('should be created correctly from JSON', () => {
    expect(new DeleteReaction(sampleDeleteReaction)).toBeJsonEqual(sampleDeleteReaction);
    const temp = {
      object: ObjectType.REACTION,
      action: ActionType.DELETE,
      reaction_id: ID,
      timestamp: TIMESTAMP,
    };
    expect(new DeleteReaction(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from JSON', () => {
    const obj = JSON.parse(dataDeleteReaction);
    expect(DeleteReaction.fromJson(obj)).toBeJsonEqual(sampleDeleteReaction);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.REACTION,
      action: ActionType.NOTIFY_ADD,
      reaction_id: ID,
      timestamp: TIMESTAMP,
    };
    const createFromJson = () => DeleteReaction.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if reaction_id is undefined', () => {
      const wrongObj = () =>
        new DeleteReaction({
          reaction_id: undefined as unknown as Hash,
          timestamp: TIMESTAMP,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error is timestamp is undefined', () => {
      const wrongObj = () =>
        new DeleteReaction({
          reaction_id: ID,
          timestamp: undefined as unknown as Timestamp,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });
  });
});
