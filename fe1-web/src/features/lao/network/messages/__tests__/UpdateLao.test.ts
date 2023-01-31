import 'jest-extended';
import '__tests__/utils/matchers';

import {
  configureTestFeatures,
  mockLao,
  mockLaoId,
  mockLaoName,
  mockPublicKey,
  mockPublicKey2,
} from '__tests__/utils';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';

import { OpenedLaoStore } from '../../../store';
import { UpdateLao } from '../UpdateLao';

const TIMESTAMP = new Timestamp(1609542000); // 2nd january 2021
const mockWitnesses = [new PublicKey(mockPublicKey), new PublicKey(mockPublicKey2)];

const sampleUpdateLao: Partial<UpdateLao> = {
  object: ObjectType.LAO,
  action: ActionType.UPDATE_PROPERTIES,
  id: mockLaoId,
  name: mockLaoName,
  last_modified: TIMESTAMP,
  witnesses: mockWitnesses,
};

const updateLaoJson = `{
  "object": "${ObjectType.LAO}",
  "action": "${ActionType.UPDATE_PROPERTIES}",
  "id": "${mockLaoId}",
  "name": "${mockLaoName}",
  "last_modified": ${TIMESTAMP},
  "witnesses": ${JSON.stringify(mockWitnesses)}
}`;

beforeAll(() => {
  configureTestFeatures();
  OpenedLaoStore.store(mockLao);
});

describe('UpdateLao', () => {
  it('should be created correctly from Json', () => {
    expect(new UpdateLao(sampleUpdateLao)).toBeJsonEqual(sampleUpdateLao);
    const temp = {
      object: ObjectType.LAO,
      action: ActionType.UPDATE_PROPERTIES,
      id: mockLaoId,
      name: mockLaoName,
      last_modified: TIMESTAMP,
      witnesses: mockWitnesses,
    };
    expect(new UpdateLao(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(updateLaoJson);
    expect(UpdateLao.fromJson(obj)).toBeJsonEqual(sampleUpdateLao);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.LAO,
      action: ActionType.UPDATE_PROPERTIES,
      id: mockLaoId,
      name: mockLaoName,
      last_modified: TIMESTAMP,
      witnesses: mockWitnesses,
    };
    const createWrongObj = () => UpdateLao.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error when name is undefined', () => {
      const createWrongObj = () =>
        new UpdateLao({
          object: ObjectType.LAO,
          action: ActionType.UPDATE_PROPERTIES,
          id: mockLaoId,
          last_modified: TIMESTAMP,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when last_modified is undefined', () => {
      const createWrongObj = () =>
        new UpdateLao({
          object: ObjectType.LAO,
          action: ActionType.UPDATE_PROPERTIES,
          id: mockLaoId,
          name: mockLaoName,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when witnesses is undefined', () => {
      const createWrongObj = () =>
        new UpdateLao({
          object: ObjectType.LAO,
          action: ActionType.UPDATE_PROPERTIES,
          id: mockLaoId,
          name: mockLaoName,
          last_modified: TIMESTAMP,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when id is undefined', () => {
      const createWrongObj = () =>
        new UpdateLao({
          object: ObjectType.LAO,
          action: ActionType.UPDATE_PROPERTIES,
          name: mockLaoName,
          last_modified: TIMESTAMP,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error when id is incorrect', () => {
      const createWrongObj = () =>
        new UpdateLao({
          object: ObjectType.LAO,
          action: ActionType.UPDATE_PROPERTIES,
          id: new Hash('id'),
          name: mockLaoName,
          last_modified: TIMESTAMP,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
