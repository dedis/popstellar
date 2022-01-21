package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.{ErrorCodes, PipelineError}

import scala.util.{Success, Try}

trait MethodContentValidator extends ContentValidator {

  /**
   * Creates a validation error message for reason <reason> that happened in
   * validator module <validator> with optional error code <errorCode>
   *
   * @param reason    the reason of the validation error
   * @param validator validator module where the error occurred
   * @param errorCode error code related to the error
   * @return a description of the error and where it occurred
   */
  override def validationError(reason: String, validator: String, rpcId: Option[Int], errorCode: ErrorCodes.ErrorCodes = ErrorCodes.INVALID_DATA): PipelineError =
    super.validationError(reason, validator, rpcId, errorCode)

  def validateChannel(channel: Channel): Boolean = channel match {
    case _ if channel.isRootChannel => true
    case _ if !channel.isSubChannel => false
    case _ => Try(channel.decodeChannelLaoId) match {
      case Success(_) => true
      case _ => false
    }
  }
}
