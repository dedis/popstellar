import { ProtocolError } from 'core/objects';

import { JsonRpcParams } from './JsonRpcParams';

export class PagedCatchup extends JsonRpcParams {
  public readonly number_of_messages: number;
  public readonly before_message_id?: string;

  constructor(params: Partial<PagedCatchup>) {
    super(params);

    if (!params.number_of_messages) {
      throw new ProtocolError("Undefined 'number_of_messages' parameter in JSON-RPC");
    } else if (isNaN(params.number_of_messages) || params.number_of_messages < 1) {
      throw new ProtocolError("Invalid 'number_of_messages' parameter in JSON-RPC");
    }
    this.number_of_messages = params.number_of_messages;
    this.before_message_id = params.before_message_id;
  }

  public static fromJson(obj: any): JsonRpcParams {
    // Schema validation already passed at top level
    return new PagedCatchup(obj);
  }
}
