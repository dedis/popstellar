import 'jest-extended';

import '__tests__/utils/matchers';
import { Base64UrlData, Hash, Timestamp, ProtocolError } from 'model/objects';
import { ActionType, ObjectType } from 'model/network/method/message/data/MessageData';

import { AddReaction } from '../AddReaction';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const mockMessageId = Base64UrlData.encode('message_id');
const ID = new Hash(mockMessageId.toString());
const REACTION = '❤️';

const sampleAddReaction: Partial<AddReaction> = {
  object: ObjectType.REACTION,
  action: ActionType.ADD,
  reaction_codepoint: REACTION,
  chirp_id: ID,
  timestamp: TIMESTAMP,
};

const dataAddReaction = `{
  "object": "${ObjectType.REACTION}",
  "action": "${ActionType.ADD}",
  "reaction_codepoint": "${REACTION}",
  "chirp_id": "${ID}",
  "timestamp": ${TIMESTAMP}
  }`;

describe('AddReaction', () => {
  it('should be created correctly from JSON', () => {
    expect(new AddReaction(sampleAddReaction)).toBeJsonEqual(sampleAddReaction);
    const temp = {
      object: ObjectType.REACTION,
      action: ActionType.ADD,
      reaction_codepoint: REACTION,
      chirp_id: ID,
      timestamp: TIMESTAMP,
    };
    expect(new AddReaction(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from JSON', () => {
    const obj = JSON.parse(dataAddReaction);
    expect(AddReaction.fromJson(obj)).toBeJsonEqual(sampleAddReaction);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.REACTION,
      action: ActionType.NOTIFY_ADD,
      reaction_codepoint: REACTION,
      chirp_id: ID,
      timestamp: TIMESTAMP,
    };
    const createFromJson = () => AddReaction.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if reaction_codepoint is undefined', () => {
      const wrongObj = () =>
        new AddReaction({
          object: ObjectType.REACTION,
          action: ActionType.ADD,
          chirp_id: ID,
          timestamp: TIMESTAMP,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is undefined', () => {
      const wrongObj = () =>
        new AddReaction({
          object: ObjectType.REACTION,
          action: ActionType.ADD,
          reaction_codepoint: REACTION,
          timestamp: TIMESTAMP,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error is timestamp is undefined', () => {
      const wrongObj = () =>
        new AddReaction({
          object: ObjectType.REACTION,
          action: ActionType.ADD,
          reaction_codepoint: REACTION,
          chirp_id: ID,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });
  });
});
