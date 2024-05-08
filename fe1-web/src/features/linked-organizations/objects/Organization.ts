import { validateFederationExchange } from 'core/network/validation/Validator';
import { Hash, HashState, ProtocolError, PublicKey, PublicKeyState } from 'core/objects';
import { OmitMethods } from 'core/types';

import { Challenge, ChallengeState } from './Challenge';

export interface OrganizationState {
  lao_id: HashState;
  server_address: string;
  public_key: PublicKeyState;
  challenge: ChallengeState;
}

export class Organization {
  public readonly lao_id: Hash;

  public readonly server_address: string;

  public readonly public_key: PublicKey;

  public readonly challenge: Challenge;

  constructor(org: OmitMethods<Organization>) {
    if (org === undefined || org === null) {
      throw new Error(
        'Error encountered while creating an Organization object: undefined/null parameters',
      );
    }
    if (org.lao_id === undefined) {
      throw new Error("Undefined 'lao_id' when creating 'Organization'");
    }
    if (org.server_address === undefined) {
      throw new Error("Undefined 'server_address' when creating 'Organization'");
    }
    if (org.public_key === undefined) {
      throw new Error("Undefined 'public_key' when creating 'Organization'");
    }
    if (org.challenge === undefined) {
      throw new Error("Undefined 'challenge' when creating 'Organization'");
    }

    this.lao_id = org.lao_id;
    this.server_address = org.server_address;
    this.public_key = org.public_key;
    this.challenge = org.challenge;
  }

  public toState(): OrganizationState {
    return {
      lao_id: this.lao_id.toState(),
      server_address: this.server_address,
      public_key: this.public_key.toState(),
      challenge: this.challenge.toState(),
    };
  }

  public static fromState(orgState: OrganizationState): Organization {
    return new Organization({
      lao_id: Hash.fromState(orgState.lao_id),
      server_address: orgState.server_address,
      public_key: PublicKey.fromState(orgState.public_key),
      challenge: Challenge.fromState(orgState.challenge),
    });
  }

  public static fromJson(obj: any): Organization {
    const { errors } = validateFederationExchange(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid Linked Organization QR Code\n\n${errors}`);
    }
    return new Organization({
      lao_id: new Hash(obj.lao_id),
      server_address: obj.server_address,
      public_key: new PublicKey(obj.public_key),
      challenge: Challenge.fromJson(obj.challenge),
    });
  }

  public toJson(): string {
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
}
