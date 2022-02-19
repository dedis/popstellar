import { validateConnectToLao } from 'core/network/validation';
import { ProtocolError } from 'core/objects/ProtocolError';

/**
 * Object containing the server url and Lao id to generate the QR code of a Lao.
 */
export class ConnectToLao {
  public readonly server: string;

  public readonly lao: string;

  constructor(connectToLao: Partial<ConnectToLao>) {
    if (connectToLao === undefined || connectToLao === null) {
      throw new ProtocolError(
        'Error encountered while creating a ConnectToLao object: undefined/null parameters',
      );
    }

    if (connectToLao.lao === undefined) {
      throw new ProtocolError("undefined 'lao' when creating 'ConnectToLao'");
    }

    if (connectToLao.server === undefined) {
      throw new ProtocolError("undefined 'server' when creating 'ConnectToLao'");
    }

    this.lao = connectToLao.lao;
    this.server = connectToLao.server;
  }

  public static fromJson(obj: any): ConnectToLao {
    const { errors } = validateConnectToLao(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid connection to lao\n\n${errors}`);
    }

    return new ConnectToLao({
      ...obj,
    });
  }
}
