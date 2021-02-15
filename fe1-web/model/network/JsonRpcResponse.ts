export abstract class JsonRpcResponse {
  public readonly id: number;

  constructor(resp: Partial<JsonRpcResponse>) {
    this.id = resp.id || -1;
  }
}
