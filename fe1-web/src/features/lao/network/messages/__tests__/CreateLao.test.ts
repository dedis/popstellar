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
import { Hash, ProtocolError, PublicKey } from 'core/objects';

import { CreateLao } from '../CreateLao';

const mockWitnesses = [new PublicKey(mockPublicKey), new PublicKey(mockPublicKey2)];

const sampleCreateLao: Partial<CreateLao> = {
  object: ObjectType.LAO,
  action: ActionType.CREATE,
  id: mockLaoId,
  name: mockLaoName,
  creation: mockLaoCreationTime,
  organizer: org,
  witnesses: mockWitnesses,
};

const createLaoJson = `{
  "object": "${ObjectType.LAO}",
  "action": "${ActionType.CREATE}",
  "id": "${mockLaoId}",
  "name": "${mockLaoName}",
  "creation": ${mockLaoCreationTime},
  "organizer": "${org}",
  "witnesses": ${JSON.stringify(mockWitnesses)}
}`;

describe('CreateLao', () => {
  beforeAll(configureTestFeatures);

  it('should be created correctly from Json', () => {
    expect(new CreateLao(sampleCreateLao)).toBeJsonEqual(sampleCreateLao);
    const temp = {
      object: ObjectType.LAO,
      action: ActionType.CREATE,
      id: mockLaoId,
      name: mockLaoName,
      creation: mockLaoCreationTime,
      organizer: org,
      witnesses: mockWitnesses,
    };
    expect(new CreateLao(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from Json', () => {
    const obj = JSON.parse(createLaoJson);
    expect(CreateLao.fromJson(obj)).toBeJsonEqual(sampleCreateLao);
  });

  it('fromJson should throw an error if the Json has incorrect action', () => {
    const obj = {
      object: ObjectType.LAO,
      action: ActionType.CREATE,
      id: mockLaoId,
      name: mockLaoName,
      creation: mockLaoCreationTime,
      organizer: org,
      witnesses: mockWitnesses,
    };
    const createWrongObj = () => CreateLao.fromJson(obj);
    expect(createWrongObj).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if name is undefined', () => {
      const createWrongObj = () =>
        new CreateLao({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          creation: mockLaoCreationTime,
          organizer: org,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if creation is undefined', () => {
      const createWrongObj = () =>
        new CreateLao({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          name: mockLaoName,
          organizer: org,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if witnesses is undefined', () => {
      const createWrongObj = () =>
        new CreateLao({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          organizer: org,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if organizer is undefined', () => {
      const createWrongObj = () =>
        new CreateLao({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: mockLaoId,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is undefined', () => {
      const createWrongObj = () =>
        new CreateLao({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          name: mockLaoName,
          creation: mockLaoCreationTime,
          organizer: org,
          witnesses: [],
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if id is incorrect', () => {
      const createWrongObj = () =>
        new CreateLao({
          object: ObjectType.LAO,
          action: ActionType.CREATE,
          id: new Hash('id'),
          name: mockLaoName,
          creation: mockLaoCreationTime,
          organizer: org,
          witnesses: mockWitnesses,
        });
      expect(createWrongObj).toThrow(ProtocolError);
    });
  });
});
