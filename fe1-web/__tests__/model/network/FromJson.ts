/* eslint-disable */

import {
  ActionType, CloseRollCall,
  CreateLao,
  CreateMeeting, CreateRollCall,
  ObjectType, OpenRollCall,
  StateLao, StateMeeting,
  UpdateLao, WitnessMessage
} from '../../../Model/Network/Method/Message/data';
import { ProtocolError } from '../../../Model/Network';
import {Base64Data, Hash, PrivateKey, PublicKey, Signature, Timestamp} from "../../../Model/Objects";
import { sign } from "tweetnacl";
import { encodeBase64 } from "tweetnacl-util";

const assert = require('assert');
const assertChai = require('chai').assert;

const STALE_TIMESTAMP = new Timestamp(1514761200);    // 1st january 2018
const STANDARD_TIMESTAMP = new Timestamp(1609455600); // 1st january 2021
const CLOSE_TIMESTAMP = new Timestamp(1609542000);    // 2nd january 2021
const FUTURE_TIMESTAMP = new Timestamp(1735686000);   // 1st january 2025

// FIXME use real keys from key pair once operational
export const mockPublicKey = new PublicKey('xjHAz+d0udy1XfHp5qugskWJVEGZETN/8DV3+ccOFSs=');
export const mockSecretKey = new PrivateKey('vx0b2hbxwPBQzfPu9NdlCcYmuFjhUFuIUDx6doHRCM7GMcDP53S53LVd8enmq6CyRYlUQZkRM3/wNXf5xw4VKw==');


const _generateKeyPair = () => {
  const pair = sign.keyPair();
  const keys = { pubKey: encodeBase64(pair.publicKey), secKey: encodeBase64(pair.secretKey) };
  return { pubKey: new PublicKey(keys.pubKey), secKey: new PrivateKey(keys.secKey) };
};





describe('=== fromJson object checks ===', function() {

  const sampleKey1: PublicKey = _generateKeyPair().pubKey;
  const sampleKey2: PublicKey = _generateKeyPair().pubKey;

  const mockMessageId = Base64Data.encode('message_id');

  const org = mockPublicKey;
  const time = STANDARD_TIMESTAMP;
  const name = 'PoP\'s team "LAO" or filling name';
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
    id: Hash.fromStringArray(org.toString(), time.toString(), name),
    name: name,
    last_modified: CLOSE_TIMESTAMP,
    witnesses: [sampleKey1, sampleKey2],
  };

  const sampleStateLao: Partial<StateLao> = {
    object: ObjectType.LAO,
    action: ActionType.STATE,
    id: Hash.fromStringArray(org.toString(), time.toString(), name),
    name: name,
    creation: time,
    last_modified: CLOSE_TIMESTAMP,
    organizer: mockPublicKey,
    witnesses: [sampleKey1, sampleKey2],
    modification_id: Hash.fromStringArray(mockMessageId.toString()),
    modification_signatures: [],
  };

  const sampleCreateMeeting: Partial<CreateMeeting> = {
    object: ObjectType.MEETING,
    action: ActionType.CREATE,
    id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
    name: name,
    creation: time,
    location: 'Lausanne',
    start: time,
    end: FUTURE_TIMESTAMP,
    extra: { extra: 'extra info' },
  };

  const sampleStateMeeting: Partial<StateMeeting> = {
    object: ObjectType.MEETING,
    action: ActionType.STATE,
    id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
    name: name,
    creation: time,
    last_modified: time,
    location: 'Lausanne',
    start: CLOSE_TIMESTAMP,
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
    creation: STANDARD_TIMESTAMP,
    start: time,
    location: 'Lausanne',
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



  it('should successfully create the objects', function () {

    // Create LAO
    assertChai.deepEqual(CreateLao.fromJson(sampleCreateLao), sampleCreateLao);
    assertChai.deepEqual(CreateLao.fromJson({
      id: Hash.fromStringArray(org.toString(), time.toString(), name),
      name: name,
      creation: time,
      organizer: org,
      witnesses: [],
    }), sampleCreateLao);


    // Update LAO
    assertChai.deepEqual(UpdateLao.fromJson(sampleUpdateLao), sampleUpdateLao);


    // State LAO
    assertChai.deepEqual(StateLao.fromJson(sampleStateLao), sampleStateLao);


    // Create Meeting
    assertChai.deepEqual(CreateMeeting.fromJson(sampleCreateMeeting), sampleCreateMeeting);
    temp = {
      object: ObjectType.MEETING,
      action: ActionType.CREATE,
      id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
      name: name,
      creation: time,
      start: time,
      end: FUTURE_TIMESTAMP,
      extra: { extra: 'extra info' },
    };
    assertChai.deepEqual(CreateMeeting.fromJson(temp), temp);
    temp = {
      object: ObjectType.MEETING,
      action: ActionType.CREATE,
      id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
      name: name,
      creation: time,
      location: 'Lausanne',
      start: time,
      extra: { extra: 'extra info' },
    };
    assertChai.deepEqual(CreateMeeting.fromJson(temp), temp);
    temp = {
      object: ObjectType.MEETING,
      action: ActionType.CREATE,
      id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
      name: name,
      creation: time,
      location: 'Lausanne',
      start: time,
      end: FUTURE_TIMESTAMP,
    };
    assertChai.deepEqual(CreateMeeting.fromJson(temp), temp);


    // State Meeting
    assertChai.deepEqual(StateMeeting.fromJson(sampleStateMeeting), sampleStateMeeting);
    temp = {
      object: ObjectType.MEETING,
      action: ActionType.STATE,
      id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
      name: name,
      creation: time,
      last_modified: time,
      start: CLOSE_TIMESTAMP,
      end: FUTURE_TIMESTAMP,
      extra: { extra: 'extra info' },
      modification_id: Hash.fromStringArray(mockMessageId.toString()),
      modification_signatures: [],
    };
    assertChai.deepEqual(StateMeeting.fromJson(temp), temp);
    temp = {
      object: ObjectType.MEETING,
      action: ActionType.STATE,
      id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
      name: name,
      creation: time,
      last_modified: time,
      location: 'Lausanne',
      start: CLOSE_TIMESTAMP,
      extra: { extra: 'extra info' },
      modification_id: Hash.fromStringArray(mockMessageId.toString()),
      modification_signatures: [],
    };
    assertChai.deepEqual(StateMeeting.fromJson(temp), temp);
    temp = {
      object: ObjectType.MEETING,
      action: ActionType.STATE,
      id: Hash.fromStringArray(mockLaoId.toString(), time.toString(), name),
      name: name,
      creation: time,
      last_modified: time,
      location: 'Lausanne',
      start: CLOSE_TIMESTAMP,
      end: FUTURE_TIMESTAMP,
      modification_id: Hash.fromStringArray(mockMessageId.toString()),
      modification_signatures: [],
    };
    assertChai.deepEqual(StateMeeting.fromJson(temp), temp);


    // Create RollCall
    assertChai.deepEqual(CreateRollCall.fromJson(sampleCreateRollCall), sampleCreateRollCall);
    temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      id: rollCallId,
      name: name,
      creation: STANDARD_TIMESTAMP,
      start: time,
      location: 'Lausanne',
    };
    assertChai.deepEqual(CreateRollCall.fromJson(temp), temp);
    temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      id: rollCallId,
      name: name,
      creation: STANDARD_TIMESTAMP,
      scheduled: time,
      location: 'Lausanne',
    };
    assertChai.deepEqual(CreateRollCall.fromJson(temp), temp);


    // Open RollCall
    assertChai.deepEqual(OpenRollCall.fromJson(sampleOpenRollCall), sampleOpenRollCall);


    // Reopen RollCall
    assertChai.deepEqual(OpenRollCall.fromJson(sampleReopenRollCall), sampleReopenRollCall);


    // Close RollCall
    assertChai.deepEqual(CloseRollCall.fromJson(sampleCloseRollCall), sampleCloseRollCall);
    temp = {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CLOSE,
      id: rollCallId,
      start: time,
      end: FUTURE_TIMESTAMP,
      attendees: [sampleKey1, sampleKey2],
    };
    assertChai.deepEqual(CloseRollCall.fromJson(temp), temp);


    // Witness Message
    assertChai.deepEqual(WitnessMessage.fromJson(sampleWitnessMessage), sampleWitnessMessage);
  });

  it('should fail (throw) during object creation', function () {

    // empty partial object
    expect(() => {
      CreateLao.fromJson({});
    }).toThrow(ProtocolError);

    // omitted a mandatory parameter (name)
    expect(() => {
      CreateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.CREATE,
        id: mockLaoId,
        creation: time,
        organizer: org,
        witnesses: [],
      });
    }).toThrow(ProtocolError);

    // garbage type (creation)
    expect(() => {
      CreateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.CREATE,
        id: mockLaoId,
        creation: "time",
        organizer: org,
        witnesses: [],
      });
    }).toThrow(ProtocolError);

    // garbage witnesses (witnesses)
    expect(() => {
      CreateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.CREATE,
        id: mockLaoId,
        creation: time,
        organizer: org,
        witnesses: ["key1"],
      });
    }).toThrow(ProtocolError);

    // garbage id (id)
    expect(() => {
      CreateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.CREATE,
        id: Base64Data.encode('garbage id'),
        creation: time,
        organizer: org,
        witnesses: ["key1"],
      });
    }).toThrow(ProtocolError);

    // stale timestamp (creation)
    expect(() => {
      CreateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.CREATE,
        id: mockLaoId,
        creation: STALE_TIMESTAMP,
        organizer: org,
        witnesses: [],
      });
    }).toThrow(ProtocolError);

    // negative timestamp (creation)
    expect(() => {
      CreateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.CREATE,
        id: mockLaoId,
        creation: new Timestamp(-42),
        organizer: org,
        witnesses: [],
      });
    }).toThrow(ProtocolError);

    // last modified before creation
    expect(() => {
      UpdateLao.fromJson({
        object: ObjectType.LAO,
        action: ActionType.UPDATE_PROPERTIES,
        id: Hash.fromStringArray(org.toString(), time.toString(), name),
        name: name,
        last_modified: STALE_TIMESTAMP,
        witnesses: [sampleKey1, sampleKey2],
      });
    }).toThrow(ProtocolError);

    // last modified before creation
    expect(() => {
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
    }).toThrow(ProtocolError);

    // garbage optional field type (location)
    // FIXME uncomment when JsonSchema is incorporated
    /*
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
    }).toThrow(ProtocolError);*/

    // end before start (end)
    expect(() => {
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
    }).toThrow(ProtocolError);

    // extra not an object (extra)
    // FIXME uncomment when JsonSchema is incorporated
    /*
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
    }).toThrow(ProtocolError);*/

    /*
    // incorrect signature
    expect(() => {
      WitnessMessage.fromJson({
        object: ObjectType.MESSAGE,
        action: ActionType.WITNESS,
        message_id: mockMessageId,
        signature: _generateKeyPair().secKey.sign(mockMessageId),
      });
    }).toThrow(ProtocolError);

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
    }).toThrow(ProtocolError);*/
  });
});
