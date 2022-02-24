import { ProtocolError } from 'core/objects';
import { validateDataObject } from 'core/network/validation';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';

import { MessageDataProperties } from 'core/types';
import { QuestionResult } from '../../objects';

/** Data sent to ask for the result of an election */
export class ElectionResult implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.RESULT;

  public readonly questions: QuestionResult[];

  constructor(msg: MessageDataProperties<ElectionResult>) {
    this.questions = msg.questions;
  }

  /**
   * Created an ElectionResult object from a given object.
   *
   * @param obj
   */
  public static fromJson(obj: any): ElectionResult {
    const { errors } = validateDataObject(ObjectType.ELECTION, ActionType.RESULT, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid election Result\n\n${errors}`);
    }

    return new ElectionResult({
      ...obj,
    });
  }
}
