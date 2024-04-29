import 'jest-extended';
import '__tests__/utils/matchers';
import { mockLaoId, mockPublicKey } from '__tests__/utils';
import { validateFederationExchange } from 'core/network/validation/Validator';
import { Hash, PublicKey, Timestamp } from 'core/objects';

import { Challenge } from '../Challenge';
import { Organization, OrganizationState } from '../Organization';

const VALID_LAO_ID: Hash = mockLaoId;
const VALID_PUBLIC_KEY: PublicKey = new PublicKey(mockPublicKey);
const VALID_HASH_VALUE = '82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d';
const VALID_TIMESTAMP = new Timestamp(123456789);
const VALID_CHALLENGE: Challenge = new Challenge({
  value: VALID_HASH_VALUE,
  valid_until: VALID_TIMESTAMP,
});
const VALID_SERVER_ADDRESS = 'wss://epfl.ch:9000/server';

describe('state and JSON round trips', () => {
  it('does a state round trip correctly', () => {
    const orgState: OrganizationState = {
      lao_id: VALID_LAO_ID.toState(),
      server_address: VALID_SERVER_ADDRESS,
      public_key: VALID_PUBLIC_KEY.toState(),
      challenge: VALID_CHALLENGE.toState(),
    };
    const org = Organization.fromState(orgState);
    expect(org.toState()).toStrictEqual(orgState);
  });

  it('does a JSON round trip correctly', () => {
    const jsonObj = {
      lao_id: VALID_LAO_ID.toString(),
      server_address: VALID_SERVER_ADDRESS,
      public_key: VALID_PUBLIC_KEY.valueOf(),
      challenge: {
        value: VALID_HASH_VALUE.toString(),
        valid_until: VALID_CHALLENGE.valid_until.valueOf(),
      },
    };
    const org = Organization.fromJson(jsonObj);
    expect(JSON.parse(org.toJson())).toStrictEqual(jsonObj);
  });
});

describe('constructor', () => {
  it('throws an error when the object is undefined', () => {
    const createOrg = () => new Organization(undefined as unknown as Organization);
    expect(createOrg).toThrow(Error);
  });

  it('throws an error when the object is null', () => {
    const createOrg = () => new Organization(null as unknown as Organization);
    expect(createOrg).toThrow(Error);
  });
});

describe('fromJson', () => {
  it('throws a ProtocolError if validation fails', () => {
    const ret = validateFederationExchange({ errors: 'Invalid data' });
    expect(ret.errors != null);
  });

  it('creates an organization correctly when validation passes', () => {
    const sampleJsonString = `{
      "lao_id": "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=",
      "public_key": "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
      "server_address": "wss://epfl.ch:9000/server",
      "challenge": {
        "value": "82520f235f413b26571529f69d53d751335873efca97e15cd7c47d063ead830d",
        "valid_until": 1714491502
      }
    }`;
    const sampleJson = JSON.parse(sampleJsonString);
    const ret = validateFederationExchange(sampleJson);
    expect(ret.errors == null);

    const org = Organization.fromJson(sampleJson);
    expect(org).toBeInstanceOf(Organization);
    expect(org.lao_id).toBeInstanceOf(Hash);
    expect(org.server_address).toEqual(VALID_SERVER_ADDRESS);
    expect(org.public_key).toBeInstanceOf(PublicKey);
    expect(org.challenge).toBeInstanceOf(Challenge);
  });
});
