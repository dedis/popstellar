import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockLaoCreationTime,
  mockLaoId,
  mockLaoName,
  mockPublicKey,
  mockPublicKey2,
  org,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';

import { StateLao } from '../StateLao';

const TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const mockWitnesses = [new PublicKey(mockPublicKey), new PublicKey(mockPublicKey2)];
const mockModificationId = Hash.fromArray('message_id');

const sampleStateLao: Partial<StateLao> = {
  object: ObjectType.LAO,
  action: ActionType.STATE,
  id: mockLaoId,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  last_modified: TIMESTAMP,
  organizer: org,
  witnesses: mockWitnesses,
  modification_id: mockModificationId,
  modification_signatures: [],
};

const stateLaoJson = `{
  "object": "${ObjectType.LAO}",
  "action": "${ActionType.STATE}",
  "id": "${mockLaoId}",
  "name": "${mockLaoName}",
  "creation": ${mockLaoCreationTime},
  "last_modified": ${TIMESTAMP},
  "organizer": "${org}",
  "witnesses": ${JSON.stringify(mockWitnesses)},
  "modification_id": "${mockModificationId}",
  "modification_signatures": []
}`;

describe('StateLao', () => {
  beforeAll(configureTestFeatures);

  it('should be created correctly from Json', () => {
    expect(new StateLao(sampleStateLao)).toBeJsonEqual(sampleStateLao);
    const temp = {
      object: ObjectType.LAO,
      action: ActionType.STATE,
      id: mockLaoId,
      name: mockLaoName,
      creation: mockLaoCreationTime,
      last_modified: TIMESTAMP,
      organizer: org,
      witnesses: mockWitnesses,
      modification_id: mockModificationId,
      modification_signatures: [],
    };
    expect(new StateLao(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(stateLaoJson);
    expect(StateLao.fromJson(obj)).toBeJsonEqual(sampleStateLao);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.LAO,
      action: ActionType.STATE,
      id: mockLaoId,
      name: mockLaoName,
      creation: mockLaoCreationTime,
      last_modified: TIMESTAMP,
      organizer: org,
      witnesses: mockWitnesses,
      modification_id: mockModificationId,
      modification_signatures: [],
    };
    const createWrongObj = () => StateLao.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if name is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if creation is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          last_modified: TIMESTAMP,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if last_modified is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if organizer is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if witnesses is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          organizer: org,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if modification_id is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          organizer: org,
          witnesses: mockWitnesses,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if modification_signatures is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is undefined', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is incorrect', () => {
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: new Hash('id'),
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if last_modified is before creation', () => {
      const TIMESTAMP_BEFORE = new Timestamp(1599999900);
      const createWrongObj = () =>
        new StateLao({
          object: ObjectType.LAO,
          action: ActionType.STATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          last_modified: TIMESTAMP_BEFORE,
          organizer: org,
          witnesses: mockWitnesses,
          modification_id: mockModificationId,
          modification_signatures: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
