import { ProtocolError } from 'core/objects';
import { validateDataObject } from 'core/network/validation';
import { ActionType, MessageData, ObjectType } from 'core/network/messages/MessageData';

import { QuestionResult } from '../../objects/Election';

/** Data sent to ask for the result of an election */
export class ElectionResult implements MessageData {
  public readonly object: ObjectType = ObjectType.ELECTION;

  public readonly action: ActionType = ActionType.RESULT;

  public readonly questions: QuestionResult[];

  constructor(msg: Partial<ElectionResult>) {
    if (!msg.questions) {
      throw new ProtocolError(
        "Undefined 'questions' parameter encountered during 'ElectionResult'",
      );
    }
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
