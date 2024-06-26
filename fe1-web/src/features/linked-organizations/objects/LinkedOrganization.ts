import { validateFederationExchange } from 'core/network/validation/Validator';
import { Hash, HashState, ProtocolError, PublicKey, PublicKeyState } from 'core/objects';
import { OmitMethods } from 'core/types';

import { Challenge, ChallengeState } from './Challenge';

export interface LinkedOrganizationState {
  lao_id: HashState;
  server_address: string;
  public_key: PublicKeyState;
  challenge?: ChallengeState;
}

export class LinkedOrganization {
  public readonly lao_id: Hash;

  public readonly server_address: string;

  public readonly public_key: PublicKey;

  public readonly challenge?: Challenge;

  constructor(org: OmitMethods<LinkedOrganization>) {
    if (org === undefined || org === null) {
      throw new Error(
        'Error encountered while creating an Linked Organization object: undefined/null parameters',
      );
    }
    if (org.lao_id === undefined) {
      throw new Error("Undefined 'lao_id' when creating 'LinkedOrganization'");
    }
    if (org.server_address === undefined) {
      throw new Error("Undefined 'server_address' when creating 'LinkedOrganization'");
    }
    if (org.public_key === undefined) {
      throw new Error("Undefined 'public_key' when creating 'LinkedOrganization'");
    }
    if (org.challenge) {
      this.challenge = org.challenge;
    }
    this.lao_id = org.lao_id;
    this.server_address = org.server_address;
    this.public_key = org.public_key;
  }

  public toState(): LinkedOrganizationState {
    if (this.challenge) {
      return {
        lao_id: this.lao_id.toState(),
        server_address: this.server_address,
        public_key: this.public_key.toState(),
        challenge: this.challenge.toState(),
      };
    }
    return {
      lao_id: this.lao_id.toState(),
      server_address: this.server_address,
      public_key: this.public_key.toState(),
    };
  }

  public static fromState(orgState: LinkedOrganizationState): LinkedOrganization {
    if (orgState.challenge) {
      return new LinkedOrganization({
        lao_id: Hash.fromState(orgState.lao_id),
        server_address: orgState.server_address,
        public_key: PublicKey.fromState(orgState.public_key),
        challenge: Challenge.fromState(orgState.challenge),
      });
    }
    return new LinkedOrganization({
      lao_id: Hash.fromState(orgState.lao_id),
      server_address: orgState.server_address,
      public_key: PublicKey.fromState(orgState.public_key),
    });
  }

  public static fromJson(obj: any): LinkedOrganization {
    const { errors } = validateFederationExchange(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid Linked Organization QR Code\n\n${errors}`);
    }
    if (obj.challenge) {
      return new LinkedOrganization({
        lao_id: new Hash(obj.lao_id),
        server_address: obj.server_address,
        public_key: new PublicKey(obj.public_key),
        challenge: Challenge.fromJson(obj.challenge),
      });
    }
    return new LinkedOrganization({
      lao_id: new Hash(obj.lao_id),
      server_address: obj.server_address,
      public_key: new PublicKey(obj.public_key),
    });
  }

  public toJson(): string {
    if (this.challenge) {
      return JSON.stringify({
        lao_id: this.lao_id,
        server_address: this.server_address,
        public_key: this.public_key.valueOf(),
        challenge: {
          value: this.challenge.value,
          valid_until: this.challenge.valid_until.valueOf(),
        },
      });
    }
    return JSON.stringify({
      lao_id: this.lao_id,
      server_address: this.server_address,
      public_key: this.public_key.valueOf(),
    });
  }
}
