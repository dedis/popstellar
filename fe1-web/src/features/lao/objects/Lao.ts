import { Hash, PublicKey, Timestamp } from 'core/objects';

import { getLaoChannel } from '../functions/channel';

export interface LaoState {
  name: string;
  id: string;
  creation: number;
  last_modified: number;
  organizer: string;
  witnesses: string[];

  // the following properties are not directly related to the PoP protocol:
  last_roll_call_id?: string;
  last_tokenized_roll_call_id?: string;

  // the addresses of all known servers hosting this lao
  server_addresses: string[];

  // the name of all channels we are subscribed to
  // workaround for https://github.com/dedis/popstellar/issues/1078
  subscribed_channels: string[];
}

export class Lao {
  public readonly name: string;

  public readonly id: Hash;

  public readonly creation: Timestamp;

  public readonly last_modified: Timestamp;

  public readonly organizer: PublicKey;

  public readonly witnesses: PublicKey[];

  // ID of the last roll call that happened in the LAO
  public last_roll_call_id?: Hash;

  // ID of the last roll call for which we have a token
  public last_tokenized_roll_call_id?: Hash;

  // Addresses of all servers of the LAO
  public server_addresses: string[];

  // Names of all channels we are subscribed to
  public subscribed_channels: string[];

  constructor(obj: Partial<Lao>) {
    if (obj === undefined || obj === null) {
      throw new Error('Error encountered while creating a LAO object: undefined/null parameters');
    }

    if (obj.id === undefined) {
      throw new Error("Undefined 'id' when creating 'LAO'");
    }
    if (obj.name === undefined) {
      throw new Error("Undefined 'name' when creating 'LAO'");
    }
    if (obj.creation === undefined) {
      throw new Error("Undefined 'creation' when creating 'LAO'");
    }
    if (obj.last_modified === undefined) {
      throw new Error("Undefined 'last_modified' when creating 'LAO'");
    }
    if (obj.organizer === undefined) {
      throw new Error("Undefined 'organizer' when creating 'LAO'");
    }
    if (obj.witnesses === undefined) {
      throw new Error("Undefined 'witnesses' when creating 'LAO'");
    }

    this.name = obj.name;
    this.id = obj.id;
    this.last_roll_call_id = obj.last_roll_call_id;
    this.creation = obj.creation;
    this.last_modified = obj.last_modified;
    this.organizer = obj.organizer;
    this.witnesses = [...obj.witnesses];
    this.last_tokenized_roll_call_id = obj.last_tokenized_roll_call_id;
    this.server_addresses = obj.server_addresses || [];

    const laoChannel = getLaoChannel(obj.id);
    if (!laoChannel) {
      throw new Error(`Obtained invalid lao channel from valid lao id '${obj.id.valueOf()}'???`);
    }

    this.subscribed_channels = obj.subscribed_channels || [laoChannel];
  }

  public static fromState(lao: LaoState): Lao {
    return new Lao({
      name: lao.name,
      id: Hash.fromState(lao.id),
      last_roll_call_id: lao.last_roll_call_id ? Hash.fromState(lao.last_roll_call_id) : undefined,
      creation: Timestamp.fromState(lao.creation),
      last_modified: Timestamp.fromState(lao.last_modified),
      organizer: PublicKey.fromState(lao.organizer),
      witnesses: lao.witnesses.map((w) => PublicKey.fromState(w)),
      last_tokenized_roll_call_id: lao.last_tokenized_roll_call_id
        ? Hash.fromState(lao.last_tokenized_roll_call_id)
        : undefined,
      server_addresses: lao.server_addresses,
      subscribed_channels: lao.subscribed_channels,
    });
  }

  public toState(): LaoState {
    return {
      name: this.name,
      id: this.id.toState(),
      creation: this.creation.toState(),
      last_modified: this.last_modified.toState(),
      organizer: this.organizer.toState(),
      witnesses: this.witnesses.map((w) => w.toState()),

      // the following properties are not directly related to the PoP protocol:
      last_roll_call_id: this.last_roll_call_id?.toState(),
      last_tokenized_roll_call_id: this.last_tokenized_roll_call_id?.toState(),

      // the addresses of all known servers hosting this lao
      server_addresses: this.server_addresses,

      // the name of all channels we are subscribed to
      // workaround for https://github.com/dedis/popstellar/issues/1078
      subscribed_channels: this.subscribed_channels,
    };
  }
}
