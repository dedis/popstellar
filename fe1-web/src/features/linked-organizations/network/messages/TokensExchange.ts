import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, PublicKey, Timestamp } from 'core/objects';

/** Data sent to exchange tokens */
export class TokensExchange implements MessageData {
  public readonly object: ObjectType = ObjectType.FEDERATION;

  public readonly action: ActionType = ActionType.TOKENS_EXCHANGE;

  public readonly lao_id: Hash;

  public readonly roll_call_id: Hash;

  public readonly tokens: PublicKey[];

  public readonly timestamp: Timestamp;

  constructor(msg: Partial<TokensExchange>) {
    if (!msg.lao_id) {
      throw new ProtocolError("Undefined 'lao_id' parameter encountered during 'TokensExchange'");
    }
    if (!msg.roll_call_id) {
      throw new ProtocolError(
        "Undefined 'roll_call_id' parameter encountered during 'TokensExchange'",
      );
    }
    if (!msg.tokens || msg.tokens.length === 0) {
      throw new ProtocolError(
        "Undefined or empty 'tokens' parameter encountered during 'TokensExchange'",
      );
    }
    if (!msg.timestamp) {
      throw new ProtocolError(
        "Undefined 'timestamp' parameter encountered during 'TokensExchange'",
      );
    }
    this.lao_id = msg.lao_id;
    this.roll_call_id = msg.roll_call_id;
    this.tokens = msg.tokens;
    this.timestamp = msg.timestamp;
  }

  /**
   * Creates an TokensExchange object from a given object
   * @param obj
   */
  public static fromJson(obj: any): TokensExchange {
    const { errors } = validateDataObject(ObjectType.FEDERATION, ActionType.TOKENS_EXCHANGE, obj);
    if (errors !== null) {
      throw new ProtocolError(`Invalid tokens exchange\n\n${errors}`);
    }

    return new TokensExchange({
      lao_id: obj.lao_id,
      roll_call_id: obj.roll_call_id,
      tokens: obj.tokens,
      timestamp: obj.timestamp,
    });
  }
}
