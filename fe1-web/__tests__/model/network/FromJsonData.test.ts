import 'jest-extended';
import '../../utils/matchers';

import {
  ActionType, CloseRollCall,
  CreateLao,
  CreateMeeting, CreateRollCall,
  ObjectType, OpenRollCall, ReopenRollCall,
  StateLao, StateMeeting,
  UpdateLao, WitnessMessage,
} from 'model/network/method/message/data';
import { storeInit } from 'store/Storage';
import { ProtocolError } from 'model/network';
import {
  Base64Data, Hash, Lao, PrivateKey, PublicKey, Timestamp,
} from 'model/objects';
import { sign } from 'tweetnacl';
import { encodeBase64 } from 'tweetnacl-util';
import { eventTags } from 'network/WebsocketUtils';
import { OpenedLaoStore } from 'store';

const STALE_TIMESTAMP = new Timestamp(1514761200); // 1st january 2018
const STANDARD_TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const FUTURE_TIMESTAMP = new Timestamp(1735686000); // 1st january 2025

export const mockPublicKey = new PublicKey('xjHAz+d0udy1XfHp5qugskWJVEGZETN/8DV3+ccOFSs=');
export const mockSecretKey = new PrivateKey('vx0b2hbxwPBQzfPu9NdlCcYmuFjhUFuIUDx6doHRCM7GMcDP53S53LVd8enmq6CyRYlUQZkRM3/wNXf5xw4VKw==');

const _generateKeyPair = () => {
  const pair = sign.keyPair();
  const keys = { pubKey: encodeBase64(pair.publicKey), secKey: encodeBase64(pair.secretKey) };
  return { pubKey: new PublicKey(keys.pubKey), secKey: new PrivateKey(keys.secKey) };
};

describe('=== fromJsonData checks ===', () => {
  beforeAll(() => {
    storeInit();

    const sampleLao: Lao = new Lao({
      name,
      id: Hash.fromStringArray(org.toString(), time.toString(), name),
      creation: time,
      last_modified: time,
      organizer: org,
      witnesses: [],
    });

    OpenedLaoStore.store(sampleLao);
  });

  const sampleKey1: PublicKey = _generateKeyPair().pubKey;
  const sampleKey2: PublicKey = _generateKeyPair().pubKey;

  const mockMessageId = Base64Data.encode('message_id');

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

  const rollCallId: Hash = Hash.fromStringArray('R', mockLaoId.toString(), time.toString(), name.toString());
  const sampleCreateRollCall: Partial<CreateRollCall> = {
    object: ObjectType.ROLL_CALL,
    action: ActionType.CREATE,
    id: rollCallId,
    name: name,
    creation: time,
    start: time,
    location: location,
    roll_call_description: 'description du rc',
  };

  const sampleOpenRollCall: Partial<OpenRollCall> = {
    object: ObjectType.ROLL_CALL,
    action: ActionType.OPEN,
    id: rollCallId,
    start: time,
  };

  const sampleReopenRollCall: Partial<OpenRollCall> = {
    object: ObjectType.ROLL_CALL,
    action: ActionType.REOPEN,
    id: rollCallId,
    start: time,
  };

  const sampleCloseRollCall: Partial<CloseRollCall> = {
    object: ObjectType.ROLL_CALL,
    action: ActionType.CLOSE,
    id: rollCallId,
    start: time,
    end: FUTURE_TIMESTAMP,
    attendees: [],
  };

  const sampleWitnessMessage: Partial<WitnessMessage> = {
    object: ObjectType.MESSAGE,
    action: ActionType.WITNESS,
    message_id: mockMessageId,
    signature: mockSecretKey.sign(mockMessageId),
  };

  const _dataLao: string = `{"object": "${ObjectType.LAO}","action": "F_ACTION",FF_MODIFICATION"id": "${mockLaoId.toString()}","name": "${name}","creation": ${time.toString()},"last_modified": ${CLOSE_TIMESTAMP.toString()},"organizer": "${org.toString()}","witnesses": []}`;
  const _dataMeeting: string = `{"object": "${ObjectType.MEETING}","action": "F_ACTION",FF_MODIFICATION"id": "${meetingId.toString()}","name": "${name}","creation": ${time},"last_modified": ${time},"location": "${location}","start": ${time},"end": ${FUTURE_TIMESTAMP.toString()},"extra": { "extra": "extra info" }}`;
  const _dataRollCall: string = `{"object": "${ObjectType.ROLL_CALL}","action": "F_ACTION",FF_MODIFICATION"id": "${rollCallId.toString()}"}`;
  const dataUpdateLao: string = `{"object": "${ObjectType.LAO}","action": "${ActionType.UPDATE_PROPERTIES}","name": "${name}","id": "${mockLaoId.toString()}","last_modified": ${CLOSE_TIMESTAMP.toString()},"witnesses": ["${sampleKey1.toString()}", "${sampleKey2.toString()}"]}`;
  const dataWitnessMessage: string = `{"object": "${ObjectType.MESSAGE}","action": "${ActionType.WITNESS}","message_id": "${mockMessageId.toString()}","signature": "${mockSecretKey.sign(mockMessageId).toString()}"}`;

  const dataCreateLao: string = _dataLao
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', '')
    .replace(/"last_modified": [0-9]*,/g, '');
  const dataBroadcastLao: string = _dataLao
    .replace('F_ACTION', ActionType.STATE)
    .replace(
      'FF_MODIFICATION',
      `\"modification_id\":\"${Hash.fromStringArray(mockMessageId.toString()).toString()}\",\"modification_signatures\":[],`,
    ).replace('"witnesses": []', `\"witnesses\": ["${sampleKey1.toString()}", "${sampleKey2.toString()}"]`);
  const dataCreateMeeting: string = _dataMeeting
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', '')
    .replace(/"last_modified": [0-9]*,/g, '');
  const dataBroadcastMeeting: string = _dataMeeting
    .replace('F_ACTION', ActionType.STATE)
    .replace(
      'FF_MODIFICATION',
      `\"modification_id\":\"${Hash.fromStringArray(mockMessageId.toString()).toString()}\",\"modification_signatures\":[],`,
    );
  const dataCreateRollCall: string = _dataRollCall
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', `\"name\":\"${name}\",\"creation\":${time},\"start\":${time},\"location\":\"${location}\",\"roll_call_description\":\"description du rc\",`);
  const dataOpenRollCall: string = _dataRollCall
    .replace('F_ACTION', ActionType.OPEN)
    .replace('FF_MODIFICATION', `\"start\":${time},`);
  const dataReopenRollCall: string = _dataRollCall
    .replace('F_ACTION', ActionType.REOPEN)
    .replace('FF_MODIFICATION', `\"start\":${time},`);
  const dataCloseRollCall: string = _dataRollCall
    .replace('F_ACTION', ActionType.CLOSE)
    .replace('FF_MODIFICATION', `\"start\":${time},\"end\":${FUTURE_TIMESTAMP.toString()},\"attendees\":[],`);

  describe('should successfully create objects from Json', () => {
    describe('using JS objects', () => {
      // Create LAO
      it('\'CreateLao\'', () => {
        expect(CreateLao.fromJson(sampleCreateLao)).toBeJsonEqual(sampleCreateLao);
        expect(CreateLao.fromJson({
          id: Hash.fromStringArray(org.toString(), time.toString(), name),
          name: name,
          creation: time,
          organizer: org,
          witnesses: [],
        })).toBeJsonEqual(sampleCreateLao);
      });

      // Update LAO
      it('\'UpdateLao\'', () => {
        expect(UpdateLao.fromJson(sampleUpdateLao)).toBeJsonEqual(sampleUpdateLao);
      });

      // State LAO
      it('\'StateLao\'', () => {
        expect(StateLao.fromJson(sampleStateLao)).toBeJsonEqual(sampleStateLao);
      });

      // Create Meeting
      it('\'CreateMeeting\'', () => {
        const calcId = Hash.fromStringArray(eventTags.MEETING, mockLaoId.toString(), time.toString(), name);
        expect(CreateMeeting.fromJson(sampleCreateMeeting)).toBeJsonEqual(sampleCreateMeeting);
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
        expect(CreateMeeting.fromJson(temp)).toBeJsonEqual(temp);
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
        expect(CreateMeeting.fromJson(temp)).toBeJsonEqual(temp);
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
        expect(CreateMeeting.fromJson(temp)).toBeJsonEqual(temp);
      });

      // State Meeting
      it('\'StateMeeting\'', () => {
        const calcId = Hash.fromStringArray(eventTags.MEETING, mockLaoId.toString(), time.toString(), name);
        expect(StateMeeting.fromJson(sampleStateMeeting)).toBeJsonEqual(sampleStateMeeting);
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
        expect(StateMeeting.fromJson(temp)).toBeJsonEqual(temp);
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
        expect(StateMeeting.fromJson(temp)).toBeJsonEqual(temp);
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
        expect(StateMeeting.fromJson(temp)).toBeJsonEqual(temp);
      });

      // Create RollCall
      it('\'CreateRollCall\'', () => {
        expect(CreateRollCall.fromJson(sampleCreateRollCall)).toBeJsonEqual(sampleCreateRollCall);
        temp = {
          object: ObjectType.ROLL_CALL,
          action: ActionType.CREATE,
          id: rollCallId,
          name: name,
          creation: STANDARD_TIMESTAMP,
          start: time,
          location: 'Lausanne',
        };
        expect(CreateRollCall.fromJson(temp)).toBeJsonEqual(temp);
        temp = {
          object: ObjectType.ROLL_CALL,
          action: ActionType.CREATE,
          id: rollCallId,
          name: name,
          creation: STANDARD_TIMESTAMP,
          scheduled: time,
          location: 'Lausanne',
        };
        expect(CreateRollCall.fromJson(temp)).toBeJsonEqual(temp);
      });

      // Open RollCall
      it('\'OpenRollCall\'', () => {
        expect(OpenRollCall.fromJson(sampleOpenRollCall)).toBeJsonEqual(sampleOpenRollCall);
      });

      // Reopen RollCall
      it('\'ReopenRollCall\'', () => {
        expect(ReopenRollCall.fromJson(sampleReopenRollCall)).toBeJsonEqual(sampleReopenRollCall);
      });

      // Close RollCall
      it('\'CloseRollCall\'', () => {
        expect(CloseRollCall.fromJson(sampleCloseRollCall)).toBeJsonEqual(sampleCloseRollCall);
        temp = {
          object: ObjectType.ROLL_CALL,
          action: ActionType.CLOSE,
          id: rollCallId,
          start: time,
          end: FUTURE_TIMESTAMP,
          attendees: [sampleKey1, sampleKey2],
        };
        expect(CloseRollCall.fromJson(temp)).toBeJsonEqual(temp);
      });

      // Witness Message
      it('\'WitnessMessage\'', () => {
        expect(WitnessMessage.fromJson(sampleWitnessMessage)).toBeJsonEqual(sampleWitnessMessage);
      });
    });

    describe('using JSON strings', () => {
      /* Note : edge cases testing in "using JS objects" test case */

      // Create LAO
      it('\'CreateLao\'', () => {
        expect(CreateLao.fromJson(JSON.parse(dataCreateLao))).toBeJsonEqual(sampleCreateLao);
      });

      // Update LAO
      it('\'UpdateLao\'', () => {
        expect(UpdateLao.fromJson(JSON.parse(dataUpdateLao))).toBeJsonEqual(sampleUpdateLao);
      });

      // State LAO
      it('\'StateLao\'', () => {
        expect(StateLao.fromJson(JSON.parse(dataBroadcastLao))).toBeJsonEqual(sampleStateLao);
      });

      // Create Meeting
      it('\'CreateMeeting\'', () => {
        expect(CreateMeeting.fromJson(JSON.parse(dataCreateMeeting))).toBeJsonEqual(sampleCreateMeeting);
      });

      // State Meeting
      it('\'StateMeeting\'', () => {
        // console.log("CREATED ", StateMeeting.fromJson(JSON.parse(dataBroadcastMeeting)));
        // console.log("EXPECTED ", sampleStateMeeting);
        expect(StateMeeting.fromJson(JSON.parse(dataBroadcastMeeting))).toBeJsonEqual(sampleStateMeeting);
      });

      // Create RollCall
      it('\'CreateRollCall\'', () => {
        expect(CreateRollCall.fromJson(JSON.parse(dataCreateRollCall))).toBeJsonEqual(sampleCreateRollCall);
      });

      // Open RollCall
      it('\'OpenRollCall\'', () => {
        expect(OpenRollCall.fromJson(JSON.parse(dataOpenRollCall))).toBeJsonEqual(sampleOpenRollCall);
      });

      // Reopen RollCall
      it('\'ReopenRollCall\'', () => {
        expect(ReopenRollCall.fromJson(JSON.parse(dataReopenRollCall))).toBeJsonEqual(sampleReopenRollCall);
      });

      // Close RollCall
      it('\'CloseRollCall\'', () => {
        expect(CloseRollCall.fromJson(JSON.parse(dataCloseRollCall))).toBeJsonEqual(sampleCloseRollCall);
      });

      // Witness Message
      it('\'WitnessMessage\'', () => {
        expect(WitnessMessage.fromJson(JSON.parse(dataWitnessMessage))).toBeJsonEqual(sampleWitnessMessage);
      });
    });
  });

  describe('should fail (throw) during object creation', () => {
    // FIXME un-skip when schema implemented
    it.skip('should fail when using incomplete object', () => {
      // empty partial object
      const event = () => { CreateLao.fromJson({}); };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow('Undefined \'name\' parameter encountered during \'CreateLao\'');
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
      expect(event).toThrow("Undefined 'name' parameter encountered during 'CreateLao'");
    });

    // FIXME uncomment when JsonSchema is incorporated
    it.skip('should fail when using garbage types', () => {
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
          id: Base64Data.encode('garbage id'),
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
          id: mockLaoId,
          name: name,
          creation: STALE_TIMESTAMP,
          organizer: org,
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
          id: mockLaoId,
          name: name,
          creation: new Timestamp(-42),
          organizer: org,
          witnesses: [],
        });
      };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow('Invalid timestamp encountered: stale timestamp');

      // last modified before creation
      event = () => {
        StateLao.fromJson({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: Hash.fromStringArray(org.toString(), time.toString(), name),
          name: name,
          creation: time,
          last_modified: STALE_TIMESTAMP,
          organizer: mockPublicKey,
          witnesses: [sampleKey1, sampleKey2],
          modification_id: Hash.fromStringArray(mockMessageId.toString()),
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
          id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
          name: name,
          creation: time,
          location: 'Lausanne',
          start: time,
          end: STALE_TIMESTAMP,
          extra: { extra: 'extra info' },
        });
      };
      expect(event).toThrow(ProtocolError);
      expect(event).toThrow("Invalid timestamp encountered: 'end' parameter smaller than 'creation'"); // also caught by this test
    });

    // FIXME uncomment when JsonSchema is incorporated
    it.skip('should fail when using garbage types for optional parameters', () => {
      // garbage optional field type (location)
      expect(() => {
        CreateMeeting.fromJson({
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
          name: name,
          creation: time,
          location: 222,
          start: time,
          end: FUTURE_TIMESTAMP,
          extra: { extra: 'extra info' },
        });
      }).toThrow(ProtocolError);

      // extra not an object (extra)
      expect(() => {
        CreateMeeting.fromJson({
          object: ObjectType.MEETING,
          action: ActionType.CREATE,
          id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
          name: name,
          creation: time,
          location: 'Lausanne',
          start: time,
          end: FUTURE_TIMESTAMP,
          extra: 'extra info',
        });
      }).toThrow(ProtocolError);
    });

    it.skip('should fail when message signature is incorrect', () => {
      // incorrect signature
      expect(() => {
        WitnessMessage.fromJson({
          object: ObjectType.MESSAGE,
          action: ActionType.WITNESS,
          message_id: mockMessageId,
          signature: _generateKeyPair().secKey.sign(mockMessageId),
        });
      }).toThrow(ProtocolError);
    });

    it.skip('should fail when message id is incorrect', () => {
      // inconsistent message_id
      expect(() => {
        WitnessMessage.fromJson({
          object: ObjectType.MESSAGE,
          action: ActionType.WITNESS,
          message_id: Base64Data.encode('inconsistent message_id'),
          signature: mockSecretKey.sign(mockMessageId),
        });
      }).toThrow(ProtocolError);

      // inconsistent message_id
      expect(() => {
        WitnessMessage.fromJson({
          object: ObjectType.MESSAGE,
          action: ActionType.WITNESS,
          message_id: mockMessageId,
          signature: mockSecretKey.sign(Base64Data.encode('inconsistent message_id')),
        });
      }).toThrow(ProtocolError);
    });
  });
});
