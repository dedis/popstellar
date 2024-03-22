import { Hash, HashState } from 'core/objects';
import { OmitMethods } from 'core/types';

export interface OrganizationState {
  id: number;
  //laoId: HashState;
  name: string;
}

export class Organization {
  /* the id of the notification, is automatically assigned */
  public readonly id: number;

  /* the id of the lao this notification is associated with */
  //public readonly laoId: Hash;

  /* this field can be used to differentiate various types of notifications */
  public readonly name: string;

  constructor(org: OmitMethods<Organization>) {
    this.id = org.id;
    //this.laoId = org.laoId;
    this.name = org.name;
  }

  public toState(): OrganizationState {
    return {
      id: this.id,
      //laoId: this.laoId.toState(),
      name: this.name,
    };
  }

  public static fromState(orgState: OrganizationState): Organization {
    return new Organization({
      id: orgState.id,
      //laoId: Hash.fromState(orgState.laoId),
      name: orgState.name,
    });
  }
}
