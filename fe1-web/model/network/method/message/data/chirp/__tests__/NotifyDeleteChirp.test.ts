import 'jest-extended';
import '__tests__/utils/matchers';

import { Base64UrlData, Hash, Timestamp } from 'model/objects';
import { ProtocolError } from 'model/network/ProtocolError';
import { NotifyDeleteChirp } from '../index';
import { ActionType, ObjectType } from '../../MessageData';

const TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CHANNEL = '/root/laoID/social/senderPublicKey';
const mockMessageId = Base64UrlData.encode('message_id');
const ID = new Hash(mockMessageId.toString());

const sampleNotifyDeleteChirp: Partial<NotifyDeleteChirp> = {
  object: ObjectType.CHIRP,
  action: ActionType.NOTIFY_DELETE,
  chirp_id: ID,
  channel: CHANNEL,
  timestamp: TIMESTAMP,
};

const dataNotifyDeleteChirp = `{
    "object": "${ObjectType.CHIRP}",
    "action":"${ActionType.NOTIFY_DELETE}",
    "chirp_id": "${ID}",
    "channel": "${CHANNEL}",
    "timestamp": ${TIMESTAMP}
  }`;

describe('NotifyDeleteChirp', () => {
  it('should be created correctly from JSON', () => {
    expect(new NotifyDeleteChirp(sampleNotifyDeleteChirp)).toBeJsonEqual(sampleNotifyDeleteChirp);
    const temp = {
      object: ObjectType.CHIRP,
      action: ActionType.NOTIFY_DELETE,
      chirp_id: ID,
      channel: CHANNEL,
      timestamp: TIMESTAMP,
    };
    expect(new NotifyDeleteChirp(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from JSON', () => {
    const obj = JSON.parse(dataNotifyDeleteChirp);
    expect(NotifyDeleteChirp.fromJson(obj)).toBeJsonEqual(sampleNotifyDeleteChirp);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.CHIRP,
      action: ActionType.DELETE,
      chirp_id: ID,
      channel: CHANNEL,
      timestamp: TIMESTAMP,
    };
    const createFromJson = () => NotifyDeleteChirp.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if id is undefined', () => {
      const wrongObj = () => new NotifyDeleteChirp({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_DELETE,
        channel: CHANNEL,
        timestamp: TIMESTAMP,
      });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if timestamp is undefined', () => {
      const wrongObj = () => new NotifyDeleteChirp({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_DELETE,
        chirp_id: ID,
        channel: CHANNEL,
      });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if channel is undefined', () => {
      const wrongObj = () => new NotifyDeleteChirp({
        object: ObjectType.CHIRP,
        action: ActionType.NOTIFY_DELETE,
        chirp_id: ID,
        timestamp: TIMESTAMP,
      });
      expect(wrongObj).toThrow(ProtocolError);
    });
  });
});
