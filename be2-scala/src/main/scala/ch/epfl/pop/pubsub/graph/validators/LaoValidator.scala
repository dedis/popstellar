package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao, UpdateLao}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent._
import scala.util.{Failure, Success}

object LaoValidator extends MessageDataContentValidator {

  val laoValidator = new LaoValidator(DbActor.getInstance)

  def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateCreateLao(rpcMessage)

  sealed class LaoValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator {

    def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "CreateLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (data, hash, sender, channel) = extractData[CreateLao](rpcMessage)
          val expectedHash: Hash = Hash.fromStrings( // needs Checking in docs or protocol.
            data.organizer.base64Data.toString,
            data.creation.toString,
            data.name
          )

          runChecks(
            checkTimestampStaleness(
              rpcMessage,
              data.creation,
              validationError(s"stale 'creation' timestamp (${data.creation})")
            ),
            checkWitnesses(
              rpcMessage,
              data.witnesses,
              validationError("duplicate witnesses keys")
            ),
            checkId(
              rpcMessage,
              expectedHash,
              data.id,
              validationError("unexpected id")
            ),
            checkMsgSenderKey(
              rpcMessage,
              data.organizer,
              sender,
              validationError("unexpected organizer public key")
            ),
            checkChannel(
              rpcMessage,
              channel,
              Channel.ROOT_CHANNEL,
              validationError(s"trying to send a CreateLao message on a channel $channel other than ${Channel.ROOT_CHANNEL}")
            ),
            checkLAOName(
              rpcMessage,
              data.name,
              validationError("LAO name must not be empty")
            )
          )
        case _ => Right(validationErrorNoMessage(rpcMessage.id))
      }

    }

    /** Check if all witnesses are distinct
      *
      * @param rpcMessage
      *   rpc message to validate
      * @param witnesses
      *   witnesses to check
      * @param error
      *   the error to forward in case the witnesses are not all distinct
      * @return
      *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
      */
    private def checkWitnesses(rpcMessage: JsonRpcMessage, witnesses: List[PublicKey], error: PipelineError): GraphMessage = {
      if (validateWitnesses(witnesses))
        Left(rpcMessage)
      else
        Right(error)
    }

    /** Checks if the msg sender is the expected one
      *
      * @param rpcMessage
      *   rpc message to validate
      * @param expectedKey
      *   the expected key
      * @param msgSenderKey
      *   the rpc message sender
      * @param error
      *   the error to forward in case the sender doesn't match the expected one
      * @return
      *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
      */
    private def checkMsgSenderKey(rpcMessage: JsonRpcMessage, expectedKey: PublicKey, msgSenderKey: PublicKey, error: PipelineError): GraphMessage = {
      if (expectedKey == msgSenderKey)
        Left(rpcMessage)
      else
        Right(error)
    }

    /** Check for chan1 and chan2 equality
      *
      * @param rpcMessage
      *   rpc message to validate
      * @param chan1
      *   the first channel to compare
      * @param chan2
      *   the second channel to compare
      * @param error
      *   the error to forward in case the channels are not equals
      * @return
      *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
      */
    private def checkChannel(rpcMessage: JsonRpcMessage, chan1: Channel, chan2: Channel, error: PipelineError): GraphMessage = {
      if (chan1 == chan2)
        Left(rpcMessage)
      else
        Right(error)
    }

    /** Check if the LAO's name is empty
      *
      * @param rpcMessage
      *   rpc message to validate
      * @param name
      *   the name to check
      * @param error
      *   the error to forward in case the name si empty
      * @return
      *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
      */
    private def checkLAOName(rpcMessage: JsonRpcMessage, name: String, error: PipelineError): GraphMessage = {
      if (name.nonEmpty)
        Left(rpcMessage)
      else
        Right(error)
    }

  }

  def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "StateLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: StateLao = message.decodedData.get.asInstanceOf[StateLao]

        val expectedHash: Hash = Hash.fromStrings(data.organizer.toString, data.creation.toString, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.last_modified)) {
          Right(validationError(s"'last_modified' (${data.last_modified}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (!validateWitnesses(data.witnesses)) {
          Right(validationError("duplicate witnesses keys"))
        } else if (!validateWitnessSignatures(data.modification_signatures, data.modification_id)) {
          Right(validationError("witness key-signature pairs are not valid for the given modification_id"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "GreetLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: GreetLao = message.decodedData.get.asInstanceOf[GreetLao]

        val channel: Channel = rpcMessage.getParamsChannel

        val expectedLaoId: Hash = rpcMessage.extractLaoId

        if (expectedLaoId != data.lao) {
          Right(validationError("unexpected id, was " + data.lao + " but expected " + expectedLaoId))
        } else if (data.frontend != message.sender) {
          Right(validationError("unexpected frontend"))
        } else if (!data.address.startsWith("ws://")) {
          Right(validationError("invalid address"))
        } else if (channel != Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.lao}")) {
          Right(validationError(s"trying to write an GreetLao message on wrong channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "UpdateLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: UpdateLao = message.decodedData.get.asInstanceOf[UpdateLao]

        val channel: Channel = rpcMessage.getParamsChannel

        // FIXME get lao creation message in order to calculate "SHA256(organizer||creation||name)"
        val askLaoMessage = dbActor ? DbActor.Read(rpcMessage.getParamsChannel, ???)

        Await.ready(askLaoMessage, duration).value match {
          case Some(Success(DbActor.DbActorReadAck(None))) =>
            Right(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "validateUpdateLao failed : no CreateLao message associated found", rpcMessage.id))
          case Some(Success(DbActor.DbActorReadAck(Some(retrievedMessage)))) =>
            val laoCreationMessage = retrievedMessage.decodedData.get.asInstanceOf[CreateLao]
            // Calculate expected hash
            val expectedHash: Hash = Hash.fromStrings(
              retrievedMessage.sender.toString,
              laoCreationMessage.creation.toString,
              laoCreationMessage.name
            )

            if (!validateTimestampStaleness(data.last_modified)) {
              Right(validationError(s"stale 'last_modified' timestamp (${data.last_modified})"))
            } else if (!validateWitnesses(data.witnesses)) {
              Right(validationError("duplicate witnesses keys"))
            } else if (expectedHash != data.id) {
              Right(validationError("unexpected id"))
            } else if (!validateChannelType(ObjectType.LAO, channel)) {
              Right(validationError(s"trying to write an UpdateLao message on wrong type of channel $channel"))
            } else {
              Left(rpcMessage)
            }
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"validateUpdateLao failed : ${ex.message}", rpcMessage.getId))
          case reply                                   => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"validateUpdateLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
