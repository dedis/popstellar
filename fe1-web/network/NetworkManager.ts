import { JsonRpcRequest, JsonRpcResponse } from 'model/network';
import { NetworkConnexion } from './NetworkConnexion';

export class NetworkManager {
  private connexions: NetworkConnexion[];

  private static INSTANCE: NetworkManager;

  private constructor() {
    this.connexions = [];
  }

  public static get(): NetworkManager {
    if (this.INSTANCE === undefined) { this.INSTANCE = new NetworkManager(); }
    return this.INSTANCE;
  }

  public connect(uri: string, port: number = 8000, path: string = ''): NetworkConnexion {
    const address: string = `ws://${uri}:${port}${(path === '') ? '' : `/${path}`}`;
    const connexion: NetworkConnexion = new NetworkConnexion(address, (m) => { console.log(m); });

    this.connexions.push(connexion);
    return connexion;
  }

  public disconnect(connexion: NetworkConnexion): void;
  public disconnect(address: string): void;
  public disconnect(param: NetworkConnexion | string): void {
    if (typeof param === 'string') {
      const connexion = this.connexions.find((nc: NetworkConnexion) => nc.address === param);
      if (connexion !== undefined) this.disconnect(connexion);
    } else {
      // TODO check that this actually works once the web version is working ^^'
      const index = this.connexions.indexOf(param);
      if (index !== -1) this.connexions.splice(index, 1);
    }
  }

  public sendPayload(payload: JsonRpcRequest): void { // FIXME return promise?
    // For now, we only have 1 connexion opened at a time: the organizer server
    this.connexions[0].sendPayload(payload);
  }
}
