package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{EndElection, SetupElection}
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ElectionValidator extends MessageDataContentValidator {
  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "SetupElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: SetupElection = message.decodedData.get.asInstanceOf[SetupElection]

        // FIXME get lao creation message in order to calculate expected hash
        val f: Future[GraphMessage] = (dbActor ? DbActor.Read(Channel.rootChannel, ???)).map {
          case DbActor.DbActorReadAck(Some(retrievedMessage)) =>
            val laoCreationMessage = retrievedMessage.decodedData.get.asInstanceOf[CreateLao]
            // Calculate expected hash
            val expectedHash: Hash = Hash.fromStrings()  // FIXME get id from db

            if (!validateTimestampStaleness(data.created_at)) {
              Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
            } else if (!validateTimestampOrder(data.created_at, data.start_time)) {
              Right(validationError(s"'start_time' (${data.start_time}) timestamp is younger than 'created_at' (${data.created_at})"))
            } else if (!validateTimestampOrder(data.start_time, data.end_time)) {
              Right(validationError(s"'end_time' (${data.end_time}) timestamp is younger than 'start_time' (${data.start_time})"))
            } else if (expectedHash != data.id) {
              Right(validationError("unexpected id"))
            } else {
              Left(rpcMessage)
            }

          case DbActor.DbActorReadAck(None) =>
            Right(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "No CreateLao message associated found", rpcMessage.id))
          case DbActor.DbActorNAck(code, description) =>
            Right(PipelineError(code, description, rpcMessage.id))
          case _ =>
            Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
        }

        Await.result(f, duration)

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "ResultElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_) => Left(rpcMessage)
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "EndElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: EndElection = message.decodedData.get.asInstanceOf[EndElection]

        // FIXME get lao creation message in order to calculate expected hash
        val f: Future[GraphMessage] = (dbActor ? DbActor.Read(Channel.rootChannel, ???)).map {
          case DbActor.DbActorReadAck(Some(retrievedMessage)) =>
            val laoCreationMessage = retrievedMessage.decodedData.get.asInstanceOf[CreateLao]
            // Calculate expected hash
            val expectedHash: Hash = Hash.fromStrings() // FIXME get id from db

            if (!validateTimestampStaleness(data.created_at)) {
              Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
            } else if (expectedHash != data.election) {
              Right(validationError("unexpected id"))
            } else {
              Left(rpcMessage)
            }

          case DbActor.DbActorReadAck(None) =>
            Right(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "No CreateLao message associated found", rpcMessage.id))
          case DbActor.DbActorNAck(code, description) =>
            Right(PipelineError(code, description, rpcMessage.id))
          case _ =>
            Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
        }

        Await.result(f, duration)
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
