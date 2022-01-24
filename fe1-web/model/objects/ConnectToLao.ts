export class ConnectToLao {
  public readonly server: string;

  public readonly lao: string;

  constructor(connectToLao: Partial<ConnectToLao>) {
    if (connectToLao === undefined || connectToLao === null) {
      throw new Error('Error encountered while creating a ConnectToLao object: '
        + 'undefined/null parameters');
    }

    if (connectToLao.lao === undefined) {
      throw new Error("undefined 'lao' when creating 'ConnectToLao'");
    }

    if (connectToLao.server === undefined) {
      throw new Error("undefined 'server' when creating 'ConnectToLao'");
    }

    this.lao = connectToLao.lao;
    this.server = connectToLao.server;
  }

  public static fromJson(obj: any): ConnectToLao {
    const { errors } =
  }
}
