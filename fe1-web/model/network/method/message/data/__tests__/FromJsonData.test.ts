// this file should be heavily refactored to improve clarity and maintainability

import 'jest-extended';
import '__tests__/utils/matchers';

import keyPair from 'test_data/keypair.json';

import {
  ActionType,
  CreateLao,
  CreateMeeting,
  ObjectType,
  StateLao,
  StateMeeting,
  UpdateLao,
} from 'model/network/method/message/data/index';
import 'store/Storage';
import { ProtocolError } from 'model/network/index';
import {
  Base64UrlData,
  EventTags,
  Hash,
  KeyPair,
  Lao,
  PrivateKey,
  PublicKey,
  Timestamp,
} from 'model/objects';
import { sign } from 'tweetnacl';
import { OpenedLaoStore } from 'store';

const STALE_TIMESTAMP = new Timestamp(1514761200); // 1st january 2018
const STANDARD_TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const FUTURE_TIMESTAMP = new Timestamp(1735686000); // 1st january 2025

export const mockPublicKey = new PublicKey(keyPair.publicKey);
export const mockSecretKey = new PrivateKey(keyPair.privateKey);

const generateKeyPair = () => {
  const pair = sign.keyPair();
  return new KeyPair({
    publicKey: new PublicKey(
      Base64UrlData.fromBuffer(Buffer.from(pair.publicKey)).valueOf(),
    ),
    privateKey: new PrivateKey(
      Base64UrlData.fromBuffer(Buffer.from(pair.secretKey)).valueOf(),
    ),
  });
};

describe('=== fromJsonData checks ===', () => {
  const sampleKey1: PublicKey = generateKeyPair().publicKey;
  const sampleKey2: PublicKey = generateKeyPair().publicKey;

  const mockMessageId = Base64UrlData.encode('message_id');

  const org = mockPublicKey;
  const time = STANDARD_TIMESTAMP;
  const name = 'poof';
  const location = 'Lausanne';
  const mockLaoId: Hash = Hash.fromStringArray(org.toString(), time.toString(), name);

  let temp: any = {};

  const sampleCreateLao: Partial<CreateLao> = {
    object: ObjectType.LAO,
    action: ActionType.CREATE,
    id: mockLaoId,
    name: name,
    creation: time,
    organizer: org,
    witnesses: [],
  };

  const sampleUpdateLao: Partial<UpdateLao> = {
    object: ObjectType.LAO,
    action: ActionType.UPDATE_PROPERTIES,
    id: mockLaoId,
    name: name,
    last_modified: CLOSE_TIMESTAMP,
    witnesses: [sampleKey1, sampleKey2],
  };

  const sampleStateLao: Partial<StateLao> = {
    object: ObjectType.LAO,
    action: ActionType.STATE,
    id: mockLaoId,
    name: name,
    creation: time,
    last_modified: CLOSE_TIMESTAMP,
    organizer: mockPublicKey,
    witnesses: [sampleKey1, sampleKey2],
    modification_id: Hash.fromStringArray(mockMessageId.toString()),
    modification_signatures: [],
  };

  const meetingId: Hash = Hash.fromStringArray('M', mockLaoId.toString(), time.toString(), name);
  const sampleCreateMeeting: Partial<CreateMeeting> = {
    object: ObjectType.MEETING,
    action: ActionType.CREATE,
    id: meetingId,
    name: name,
    creation: time,
    location: location,
    start: time,
    end: FUTURE_TIMESTAMP,
    extra: { extra: 'extra info' },
  };

  const sampleStateMeeting: Partial<StateMeeting> = {
    object: ObjectType.MEETING,
    action: ActionType.STATE,
    id: meetingId,
    name: name,
    creation: time,
    last_modified: time,
    location: location,
    start: time,
    end: FUTURE_TIMESTAMP,
    extra: { extra: 'extra info' },
    modification_id: Hash.fromStringArray(mockMessageId.toString()),
    modification_signatures: [],
  };

  const rollCallId = Hash.fromStringArray('R', mockLaoId.toString(), time.toString(), name.toString());

  const rollCallUpdateId = Hash.fromStringArray('R', mockLaoId.toString(), rollCallId.toString(), time.toString());

  const dataLao: string = `{"object": "${ObjectType.LAO}","action": "F_ACTION",FF_MODIFICATION"id": "${mockLaoId.toString()}","name": "${name}","creation": ${time.toString()},"last_modified": ${CLOSE_TIMESTAMP.toString()},"organizer": "${org.toString()}","witnesses": []}`;
  const dataMeeting: string = `{"object": "${ObjectType.MEETING}","action": "F_ACTION",FF_MODIFICATION"id": "${meetingId.toString()}","name": "${name}","creation": ${time},"last_modified": ${time},"location": "${location}","start": ${time},"end": ${FUTURE_TIMESTAMP.toString()},"extra": { "extra": "extra info" }}`;
  const dataRollCall: string = `{"object": "${ObjectType.ROLL_CALL}","action":"F_ACTION",FF_MODIFICATION}`;
  const dataUpdateLao: string = `{"object": "${ObjectType.LAO}","action": "${ActionType.UPDATE_PROPERTIES}","name": "${name}","id": "${mockLaoId.toString()}","last_modified": ${CLOSE_TIMESTAMP.toString()},"witnesses": ["${sampleKey1.toString()}", "${sampleKey2.toString()}"]}`;

  const dataCreateLao: string = dataLao
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', '')
    .replace(/"last_modified": [0-9]*,/g, '');
  const dataBroadcastLao: string = dataLao
    .replace('F_ACTION', ActionType.STATE)
    .replace(
      'FF_MODIFICATION',
      `"modification_id":"${Hash.fromStringArray(mockMessageId.toString()).toString()}","modification_signatures":[],`,
    ).replace('"witnesses": []', `"witnesses": ["${sampleKey1.toString()}", "${sampleKey2.toString()}"]`);
  const dataCreateMeeting: string = dataMeeting
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', '')
    .replace(/"last_modified": [0-9]*,/g, '');
  const dataBroadcastMeeting: string = dataMeeting
    .replace('F_ACTION', ActionType.STATE)
    .replace(
      'FF_MODIFICATION',
      `"modification_id":"${Hash.fromStringArray(mockMessageId.toString()).toString()}","modification_signatures":[],`,
    );
  const dataCreateRollCall: string = dataRollCall
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', `"id": "${rollCallId.toString()}","name":"${name}","creation":${time},
    "proposed_start":${time},"proposed_end":${CLOSE_TIMESTAMP},"location":"${location}","description":"Roll Call description"`);
  const dataOpenRollCall: string = dataRollCall
    .replace('F_ACTION', ActionType.OPEN)
    .replace('FF_MODIFICATION', `"update_id":"${rollCallUpdateId.toString()}","opens":"${rollCallId.toString()}","opened_at":${time}`);

  beforeAll(() => {
    const sampleLao: Lao = new Lao({
      name,
      id: mockLaoId,
      creation: time,
      last_modified: time,
      organizer: org,
      witnesses: [],
    });

    OpenedLaoStore.store(sampleLao);
  });

  describe('should successfully create objects', () => {
    describe('from JS objects', () => {
      // Create LAO
      it('\'CreateLao\'', () => {
        expect(new CreateLao(sampleCreateLao)).toBeJsonEqual(sampleCreateLao);
        expect(new CreateLao({
          id: Hash.fromStringArray(org.toString(), time.toString(), name),
          name: name,
          creation: time,
          organizer: org,
          witnesses: [],
        })).toBeJsonEqual(sampleCreateLao);
      });

      // Update LAO
      it('\'UpdateLao\'', () => {
        expect(new UpdateLao(sampleUpdateLao)).toBeJsonEqual(sampleUpdateLao);
      });

      // State LAO
      it('\'StateLao\'', () => {
        expect(new StateLao(sampleStateLao)).toBeJsonEqual(sampleStateLao);
      });

      // Create Meeting
      it('\'CreateMeeting\'', () => {
        const calcId = Hash.fromStringArray(EventTags.MEETING,
          mockLaoId.toString(), time.toString(), name);
        expect(new CreateMeeting(sampleCreateMeeting)).toBeJsonEqual(sampleCreateMeeting);
        temp = {
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: calcId,
          name: name,
          creation: time,
          start: time,
          end: FUTURE_TIMESTAMP,
          extra: { extra: 'extra info' },
        };
        expect(new CreateMeeting(temp)).toBeJsonEqual(temp);
        temp = {
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: calcId,
          name: name,
          creation: time,
          location: 'Lausanne',
          start: time,
          extra: { extra: 'extra info' },
        };
        expect(new CreateMeeting(temp)).toBeJsonEqual(temp);
        temp = {
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: calcId,
          name: name,
          creation: time,
          location: 'Lausanne',
          start: time,
          end: FUTURE_TIMESTAMP,
        };
        expect(new CreateMeeting(temp)).toBeJsonEqual(temp);
      });

      // State Meeting
      it('\'StateMeeting\'', () => {
        const calcId = Hash.fromStringArray(EventTags.MEETING,
          mockLaoId.toString(), time.toString(), name);
        expect(new StateMeeting(sampleStateMeeting)).toBeJsonEqual(sampleStateMeeting);
        temp = {
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: calcId,
          name: name,
          creation: time,
          last_modified: time,
          start: CLOSE_TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          extra: { extra: 'extra info' },
          modification_id: Hash.fromStringArray(mockMessageId.toString()),
          modification_signatures: [],
        };
        expect(new StateMeeting(temp)).toBeJsonEqual(temp);
        temp = {
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: calcId,
          name: name,
          creation: time,
          last_modified: time,
          location: 'Lausanne',
          start: CLOSE_TIMESTAMP,
          extra: { extra: 'extra info' },
          modification_id: Hash.fromStringArray(mockMessageId.toString()),
          modification_signatures: [],
        };
        expect(new StateMeeting(temp)).toBeJsonEqual(temp);
        temp = {
          object: ObjectType.MEETING,
          action: ActionType.STATE,
          id: calcId,
          name: name,
          creation: time,
          last_modified: time,
          location: 'Lausanne',
          start: CLOSE_TIMESTAMP,
          end: FUTURE_TIMESTAMP,
          modification_id: Hash.fromStringArray(mockMessageId.toString()),
          modification_signatures: [],
        };
        expect(new StateMeeting(temp)).toBeJsonEqual(temp);
      });

    describe('from JSON objects', () => {
      /* Note : edge cases testing in "using JS objects" test case */

      // Create LAO
      it('\'CreateLao\'', () => {
        const obj = JSON.parse(dataCreateLao);
        expect(CreateLao.fromJson(obj)).toBeJsonEqual(sampleCreateLao);
      });

      // Update LAO
      it('\'UpdateLao\'', () => {
        const obj = JSON.parse(dataUpdateLao);
        expect(UpdateLao.fromJson(obj)).toBeJsonEqual(sampleUpdateLao);
      });

      // State LAO
      it('\'StateLao\'', () => {
        const obj = JSON.parse(dataBroadcastLao);
        expect(StateLao.fromJson(obj)).toBeJsonEqual(sampleStateLao);
      });

      // Create Meeting
      it('\'CreateMeeting\'', () => {
        const obj = JSON.parse(dataCreateMeeting);
        expect(CreateMeeting.fromJson(obj)).toBeJsonEqual(sampleCreateMeeting);
      });

      // State Meeting
      it('\'StateMeeting\'', () => {
        // console.log("CREATED ", StateMeeting.fromJson(obj);
        // console.log("EXPECTED ", sampleStateMeeting);
        const obj = JSON.parse(dataBroadcastMeeting);
        expect(StateMeeting.fromJson(obj)).toBeJsonEqual(sampleStateMeeting);
      });
    });
  });

  describe('should fail (throw) during object creation', () => {
    it('should fail when using incomplete object', () => {
      // empty partial object
      const event = () => { CreateLao.fromJson({}); };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow('should have required property');
    });

    it('should fail when omitting a mandatory parameter', () => {
      // omitted a mandatory parameter (name)
      const event = () => {
        CreateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          creation: time,
          organizer: org,
          witnesses: [],
        });
      };

      expect(event).toThrow(ProtocolError);
      expect(event).toThrow('should have required property \'name\'');
    });

    it('should fail when using garbage types', () => {
      // garbage type (creation)
      let event = () => {
        CreateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          name: name,
          creation: 'time',
          organizer: org,
          witnesses: [],
        });
      };
      expect(event).toThrow(ProtocolError);

      // garbage witnesses (witnesses)
      event = () => {
        CreateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          name: name,
          creation: time,
          organizer: org,
          witnesses: ['key1'],
        });
      };
      expect(event).toThrow(ProtocolError);

      // garbage id (id)
      event = () => {
        CreateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: Base64UrlData.encode('garbage id'),
          name: name,
          creation: time,
          organizer: org,
          witnesses: ['key1'],
        });
      };
      expect(event).toThrow(ProtocolError);
    });

    it('should fail when using inconsistent timestamps', () => {
      // stale timestamp (creation)
      let event = () => {
        CreateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId.valueOf(),
          name: name,
          creation: STALE_TIMESTAMP.valueOf(),
          organizer: org.valueOf(),
          witnesses: [],
        });
      };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow('Invalid timestamp encountered: stale timestamp');

      // negative timestamp (creation)
      event = () => {
        CreateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId.valueOf(),
          name: name,
          creation: new Timestamp(-42).valueOf(),
          organizer: org.valueOf(),
          witnesses: [],
        });
      };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow('creation should be >= 0');

      // last modified before creation
      event = () => {
        StateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: Hash.fromStringArray(org.toString(), time.toString(), name).valueOf(),
          name: name,
          creation: time.valueOf(),
          last_modified: STALE_TIMESTAMP.valueOf(),
          organizer: mockPublicKey.valueOf(),
          witnesses: [sampleKey1.valueOf(), sampleKey2.valueOf()],
          modification_id: Hash.fromStringArray(mockMessageId.toString()).valueOf(),
          modification_signatures: [],
        });
      };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow("Invalid timestamp encountered: 'last_modified' parameter smaller than 'creation'");

      // end before start (end)
      event = () => {
        CreateMeeting.fromJson({
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name).valueOf(),
          name: name,
          creation: time.valueOf(),
          location: 'Lausanne',
          start: time.valueOf(),
          end: STALE_TIMESTAMP.valueOf(),
          extra: { extra: 'extra info' },
        });
      };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow("Invalid timestamp encountered: 'end' parameter smaller than 'creation'"); // also caught by this test
    });

    it('should fail when using garbage types for optional parameters', () => {
      // garbage optional field type (location)
      expect(() => {
        CreateMeeting.fromJson({
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name).valueOf(),
          name: name,
          creation: time.valueOf(),
          location: 222,
          start: time.valueOf(),
          end: FUTURE_TIMESTAMP.valueOf(),
          extra: { extra: 'extra info' },
        });
      }).toThrow(ProtocolError);

      // extra not an object (extra)
      expect(() => {
        CreateMeeting.fromJson({
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name).valueOf(),
          name: name,
          creation: time.valueOf(),
          location: 'Lausanne',
          start: time.valueOf(),
          end: FUTURE_TIMESTAMP.valueOf(),
          extra: 'extra info',
        });
      }).toThrow(ProtocolError);
    });
  });
});
