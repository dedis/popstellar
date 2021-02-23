import { Message } from './message';
import { ProtocolError } from '../ProtocolError';
import { JsonRpcParams } from './JsonRpcParams';

export class JsonRpcParamsWithMessage extends JsonRpcParams {
  public readonly message: Message;

  constructor(params: Partial<JsonRpcParamsWithMessage>) {
    super(params);

    if (params.message === undefined || params.message === null) {
      throw new ProtocolError("Undefined 'message' parameter in JSON-RPC");
    }

    this.message = Message.fromJson(params.message);
  }
}
