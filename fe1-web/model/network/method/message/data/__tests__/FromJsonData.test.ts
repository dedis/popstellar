// this file should be heavily refactored to improve clarity and maintainability

import 'jest-extended';
import '__tests__/utils/matchers';

import keyPair from 'test_data/keypair.json';

import {
  ActionType,
  ObjectType,
} from 'model/network/method/message/data/index';
import 'store/Storage';
import { ProtocolError } from 'model/network/index';
import {
  Base64UrlData,
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

const mockPublicKey = new PublicKey(keyPair.publicKey);

describe('=== fromJsonData checks ===', () => {
  const org = mockPublicKey;
  const time = STANDARD_TIMESTAMP;
  const name = 'poof';
  const mockLaoId: Hash = Hash.fromStringArray(org.toString(), time.toString(), name);

  const sampleCreateLao: Partial<CreateLao> = {
    object: ObjectType.LAO,
    action: ActionType.CREATE,
    id: mockLaoId,
    name: name,
    creation: time,
    organizer: org,
    witnesses: [],
  };

  const dataLao: string = `{"object": "${ObjectType.LAO}","action": "F_ACTION",FF_MODIFICATION"id": "${mockLaoId.toString()}","name": "${name}","creation": ${time.toString()},"last_modified": ${CLOSE_TIMESTAMP.toString()},"organizer": "${org.toString()}","witnesses": []}`;

  const dataCreateLao: string = dataLao
    .replace('F_ACTION', ActionType.CREATE)
    .replace('FF_MODIFICATION', '')
    .replace(/"last_modified": [0-9]*,/g, '');

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
        expect(new CreateLao(sampleCreateLao))
          .toBeJsonEqual(sampleCreateLao);
        expect(new CreateLao({
          id: Hash.fromStringArray(org.toString(), time.toString(), name),
          name: name,
          creation: time,
          organizer: org,
          witnesses: [],
        }))
          .toBeJsonEqual(sampleCreateLao);
      });

      describe('from JSON objects', () => {
        /* Note : edge cases testing in "using JS objects" test case */

        // Create LAO
        it('\'CreateLao\'', () => {
          const obj = JSON.parse(dataCreateLao);
          expect(CreateLao.fromJson(obj))
            .toBeJsonEqual(sampleCreateLao);
        });
      });
    });

    describe('should fail (throw) during object creation', () => {
      it('should fail when using incomplete object', () => {
        // empty partial object
        const event = () => {
          CreateLao.fromJson({});
        };
        expect(event)
          .toThrow(ProtocolError);
        expect(event)
          .toThrow('should have required property');
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

        expect(event)
          .toThrow(ProtocolError);
        expect(event)
          .toThrow('should have required property \'name\'');
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
        expect(event)
          .toThrow(ProtocolError);

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
        expect(event)
          .toThrow(ProtocolError);

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
        expect(event)
          .toThrow(ProtocolError);
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
        expect(event)
          .toThrow(ProtocolError);
        expect(event)
          .toThrow('Invalid timestamp encountered: stale timestamp');

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
        expect(event)
          .toThrow(ProtocolError);
        expect(event)
          .toThrow('creation should be >= 0');
      });
    });
  });
});
