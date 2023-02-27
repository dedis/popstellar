package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao, UpdateLao}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, PublicKey, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent._
import scala.util.{Failure, Success}

object LaoValidator extends MessageDataContentValidator {

  val laoValidator = new LaoValidator(DbActor.getInstance)

  def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateCreateLao(rpcMessage)

  def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateStateLao(rpcMessage)

  def validateGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateGreetLao(rpcMessage)

  def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = laoValidator.validateUpdateLao(rpcMessage)

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

    def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "StateLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (stateLao, _, publicKey, _) = extractData[StateLao](rpcMessage)

          val expectedHash: Hash = Hash.fromStrings(
            publicKey.toString,
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
              validationError("duplicate witnesses keys")
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
              validationError("unexpected id")
            )
          )

        case _ => Right(validationErrorNoMessage(rpcMessage.id))
      }
    }

    def validateGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "GreetLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (greetLao, hash, publicKey, channel) = extractData[GreetLao](rpcMessage)
          val expectedHash = rpcMessage.extractLaoId

          runChecks(
            checkId(
              rpcMessage,
              expectedHash,
              greetLao.lao,
              validationError("unexpected id, was " + greetLao.lao + " but expected " + expectedHash)
            ),
            checkMsgSenderKey(
              rpcMessage,
              greetLao.frontend,
              publicKey,
              validationError("unexpected frontend")
            ),
            checkAddressStartWith(
              rpcMessage,
              greetLao.address,
              "ws://",
              validationError("invalid address")
            ),
            checkChannel(
              rpcMessage,
              channel,
              Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${greetLao.lao}"),
              validationError(s"trying to write an GreetLao message on wrong channel $channel")
            )
          )

        case _ => Right(validationErrorNoMessage(rpcMessage.id))
      }

    }

    def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
      def validationError(reason: String): PipelineError = super.validationError(reason, "UpdateLao", rpcMessage.id)

      rpcMessage.getParamsMessage match {
        case Some(message: Message) =>
          val (updateLao, hash, publicKey, channel) = extractData[UpdateLao](rpcMessage)

          // FIXME get lao creation message in order to calculate "SHA256(organizer||creation||name)"
          val askLaoMessage = dbActor ? DbActor.Read(channel, ???)

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

              runChecks(
                checkTimestampStaleness(
                  rpcMessage,
                  updateLao.last_modified,
                  validationError(s"stale 'last_modified' timestamp (${updateLao.last_modified})")
                ),
                checkWitnesses(
                  rpcMessage,
                  updateLao.witnesses,
                  validationError("duplicate witnesses keys")
                ),
                checkId(
                  rpcMessage,
                  expectedHash,
                  updateLao.id,
                  validationError("unexpected id")
                ),
                checkChannelType(
                  rpcMessage,
                  ObjectType.LAO,
                  channel,
                  dbActorRef,
                  validationError(s"trying to write an UpdateLao message on wrong type of channel $channel")
                )
              )

            case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"validateUpdateLao failed : ${ex.message}", rpcMessage.getId))
            case reply                                   => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"validateUpdateLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
          }

        case _ => Right(validationErrorNoMessage(rpcMessage.id))

      }
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
  private def checkWitnesses(rpcMessage: JsonRpcRequest, witnesses: List[PublicKey], error: PipelineError): GraphMessage = {
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
  private def checkMsgSenderKey(rpcMessage: JsonRpcRequest, expectedKey: PublicKey, msgSenderKey: PublicKey, error: PipelineError): GraphMessage = {
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
  private def checkChannel(rpcMessage: JsonRpcRequest, chan1: Channel, chan2: Channel, error: PipelineError): GraphMessage = {
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
  private def checkLAOName(rpcMessage: JsonRpcRequest, name: String, error: PipelineError): GraphMessage = {
    if (name.nonEmpty)
      Left(rpcMessage)
    else
      Right(error)
  }

  /** Checks witnesses key signature pairs for given modification id
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param witnessesKeyPairs
    *   the witness key signature pairs
    * @param id
    *   modification id of the message
    * @param error
    *   the error to forward in case of invalid modifications
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  private def checkWitnessesSignatures(rpcMessage: JsonRpcRequest, witnessesKeyPairs: List[WitnessSignaturePair], id: Hash, error: PipelineError): GraphMessage = {
    if (validateWitnessSignatures(witnessesKeyPairs, id))
      Left(rpcMessage)
    else
      Right(error)
  }

  /** Check if the address starts with the given string
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param address
    *   the address to check
    * @param startString
    *   the string the address must start with
    * @param error
    *   the error to forward in case the address doesn't start with the expected string
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  private def checkAddressStartWith(rpcMessage: JsonRpcRequest, address: String, startString: String, error: PipelineError): GraphMessage = {
    if (address.startsWith(startString))
      Left(rpcMessage)
    else
      Right(error)
  }
}
