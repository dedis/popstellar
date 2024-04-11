import { Hash, HashState } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface OrganizationState {
  laoId: HashState;
  name: string;
}

export class Organization {
  /*the id of the lao*/
  public readonly laoId: Hash;

  /* this field can be used to differentiate various types of notifications */
  public readonly name: string;

  constructor(org: OmitMethods<Organization>) {
    this.laoId = org.laoId;
    this.name = org.name;
  }

  public toState(): OrganizationState {
    return {
      laoId: this.laoId.toState(),
      name: this.name,
    };
  }

  public static fromState(orgState: OrganizationState): Organization {
    return new Organization({
      laoId: Hash.fromState(orgState.laoId),
      name: orgState.name,
    });
  }
}
