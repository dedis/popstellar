package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao, UpdateLao}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.PublishSubscribe
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent._
import scala.util.{Failure, Success}

object LaoValidator extends MessageDataContentValidator {

  val laoValidator = new LaoValidator(PublishSubscribe.getDbActorRef)

  def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateCreateLao(rpcMessage)

  def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateStateLao(rpcMessage)

  def validateGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateGreetLao(rpcMessage)

  def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateUpdateLao(rpcMessage)

  private val dupWitnessError = "duplicate witnesses keys"
  private val unexpectedID = "unexpected id"
  private val addressRegPat = "^.*:\\/\\/[a-zA-Z0-9.\\-_]+:?[0-9]*[a-zA-Z0-9\\/\\-_]*".r
  private val notEmptyStringPat = ".+".r

  sealed class LaoValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator {

    def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "CreateLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (createLao, _, senderPK, channel) = extractData[CreateLao](rpcMessage)
          val expectedHash: Hash = Hash.fromStrings(
            createLao.organizer.base64Data.toString,
            createLao.creation.toString,
            createLao.name
          )

          runChecks(
            checkTimestampStaleness(
              rpcMessage,
              createLao.creation,
              validationError(s"stale 'creation' timestamp (${createLao.creation})")
            ),
            checkWitnesses(
              rpcMessage,
              createLao.witnesses,
              validationError(dupWitnessError)
            ),
            checkId(
              rpcMessage,
              expectedHash,
              createLao.id,
              validationError(unexpectedID + " " + createLao.id.toString)
            ),
            checkMsgSenderKey(
              rpcMessage,
              createLao.organizer,
              senderPK,
              validationError(s"unexpected organizer public key $senderPK")
            ),
            checkChannel(
              rpcMessage,
              channel,
              Channel.ROOT_CHANNEL,
              validationError(s"trying to send a CreateLao message on a channel $channel other than ${Channel.ROOT_CHANNEL}")
            ),
            checkStringPattern(
              rpcMessage,
              createLao.name,
              notEmptyStringPat,
              validationError("LAO name must not be empty")
            )
          )
        case _ => Left(validationErrorNoMessage(rpcMessage.id))
      }

    }

    def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "StateLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (stateLao, _, senderPK, _) = extractData[StateLao](rpcMessage)

          val expectedHash: Hash = Hash.fromStrings(
            senderPK.toString,
            stateLao.creation.toString,
            stateLao.name
          )

          runChecks(
            checkTimestampStaleness(
              rpcMessage,
              stateLao.creation,
              validationError(s"stale 'creation' timestamp (${stateLao.creation})")
            ),
            checkTimestampOrder(
              rpcMessage,
              stateLao.creation,
              stateLao.last_modified,
              validationError(s"'last_modified' (${stateLao.last_modified}) timestamp is smaller than 'creation' (${stateLao.creation})")
            ),
            checkWitnesses(
              rpcMessage,
              stateLao.witnesses,
              validationError(dupWitnessError)
            ),
            checkWitnessesSignatures(
              rpcMessage,
              stateLao.modification_signatures,
              stateLao.modification_id,
              validationError("witness key-signature pairs are not valid for the given modification_id")
            ),
            checkId(
              rpcMessage,
              expectedHash,
              stateLao.id,
              validationError(unexpectedID + " " + stateLao.id.toString)
            )
          )

        case _ => Left(validationErrorNoMessage(rpcMessage.id))
      }
    }

    def validateGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "GreetLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (greetLao, _, senderPK, channel) = extractData[GreetLao](rpcMessage)
          val expectedHash = rpcMessage.extractLaoId

          runChecks(
            checkId(
              rpcMessage,
              expectedHash,
              greetLao.lao,
              validationError("unexpected id, was " + greetLao.lao + " but expected " + expectedHash)
            ),
            checkStringPattern(
              rpcMessage,
              greetLao.address,
              addressRegPat,
              validationError(s"invalid address ${greetLao.address}")
            ),
            checkChannel(
              rpcMessage,
              channel,
              Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${greetLao.lao}"),
              validationError(s"trying to write an GreetLao message on wrong channel $channel")
            )
          )

        case _ => Left(validationErrorNoMessage(rpcMessage.id))
      }

    }

    def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "UpdateLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (updateLao, _, senderPK, channel) = extractData[UpdateLao](rpcMessage)

          // FIXME get lao creation message in order to calculate "SHA256(organizer||creation||name)"
          val askLaoMessage = dbActor ? DbActor.Read(channel, ???)

          Await.ready(askLaoMessage, duration).value match {
            case Some(Success(DbActor.DbActorReadAck(None))) =>
              Left(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "validateUpdateLao failed : no CreateLao message associated found", rpcMessage.id))
            case Some(Success(DbActor.DbActorReadAck(Some(retrievedMessage)))) =>
              val laoCreationMessage = retrievedMessage.decodedData.get.asInstanceOf[CreateLao]
              // Calculate expected hash
              val expectedHash: Hash = Hash.fromStrings(
                retrievedMessage.sender.toString,
                laoCreationMessage.creation.toString,
                laoCreationMessage.name
              )

              runChecks(
                checkTimestampStaleness(
                  rpcMessage,
                  updateLao.last_modified,
                  validationError(s"stale 'last_modified' timestamp (${updateLao.last_modified})")
                ),
                checkWitnesses(
                  rpcMessage,
                  updateLao.witnesses,
                  validationError(dupWitnessError)
                ),
                checkId(
                  rpcMessage,
                  expectedHash,
                  updateLao.id,
                  validationError(unexpectedID + " " + updateLao.id.toString)
                ),
                checkChannelType(
                  rpcMessage,
                  ObjectType.LAO,
                  channel,
                  dbActorRef,
                  validationError(s"trying to write an UpdateLao message on wrong type of channel $channel")
                )
              )

            case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"validateUpdateLao failed : ${ex.message}", rpcMessage.getId))
            case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"validateUpdateLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
          }

        case _ => Left(validationErrorNoMessage(rpcMessage.id))

      }
    }
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
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  private def checkChannel(rpcMessage: JsonRpcRequest, chan1: Channel, chan2: Channel, error: PipelineError): GraphMessage = {
    if (chan1 == chan2)
      Right(rpcMessage)
    else
      Left(error)
  }
}
