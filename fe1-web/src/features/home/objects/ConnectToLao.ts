import { validateConnectToLao } from 'core/network/validation';
import { Hash, ProtocolError } from 'core/objects';

/**
 * Object containing the server url and Lao id to generate the QR code of a Lao.
 */
export class ConnectToLao {
  public readonly servers: string[];

  // is the lao id, named lao because it is the name encoded in the qr code
  public readonly lao: Hash;

  constructor(connectToLao: Partial<ConnectToLao>) {
    if (connectToLao === undefined || connectToLao === null) {
      throw new ProtocolError(
        'Error encountered while creating a ConnectToLao object: undefined/null parameters',
      );
    }

    if (connectToLao.lao === undefined) {
      throw new ProtocolError("undefined 'lao' when creating 'ConnectToLao'");
    }

    if (connectToLao.servers === undefined) {
      throw new ProtocolError("undefined 'servers' when creating 'ConnectToLao'");
    }

    this.lao = connectToLao.lao;
    this.servers = connectToLao.servers;
  }

  public static fromJson(obj: any): ConnectToLao {
    const { errors } = validateConnectToLao(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid connection to lao\n\n${errors}`);
    }

    return new ConnectToLao({
      lao: new Hash(obj.lao),
      /* in the future the qr code should encode a list of servers */
      servers: [obj.server],
    });
  }

  public toJson(): string {
    return JSON.stringify({
      lao: this.lao,
      /* in the future the qr code should encode a list of servers */
      server: this.servers[0],
    });
  }
}
