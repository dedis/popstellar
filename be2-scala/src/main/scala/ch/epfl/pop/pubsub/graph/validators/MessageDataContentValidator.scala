package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey, Timestamp, WitnessSignaturePair}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.Read

import scala.concurrent.Await
import scala.util.Success
import scala.util.matching.Regex

trait MessageDataContentValidator extends ContentValidator with AskPatternConstants {
  implicit lazy val dbActor: AskableActorRef = DbActor.getInstance

  def validationErrorNoMessage(rpcId: Option[Int]): PipelineError = PipelineError(ErrorCodes.INVALID_DATA.id, s"RPC-params does not contain any message", rpcId)

  // Lower bound for a timestamp to not be stale
  final val TIMESTAMP_BASE_TIME: Timestamp = Timestamp(1577833200L) // 1st january 2020

  /** Check whether a <timestamp> is stale or not
    *
    * @param timestamp
    *   timestamp to be checked
    * @return
    *   true iff the timestamp is not stale
    */
  final def validateTimestampStaleness(timestamp: Timestamp): Boolean = TIMESTAMP_BASE_TIME < timestamp

  // Same as validateTimestampStaleness except that it returns a GraphMessage
  def checkTimestampStaleness(rpcMessage: JsonRpcRequest, timestamp: Timestamp, error: PipelineError): GraphMessage = {
    if (validateTimestampStaleness(timestamp))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Check whether timestamp <first> is not older than timestamp <second>
    *
    * @param first
    *   first timestamp to be checked
    * @param second
    *   second timestamp to be checked
    * @return
    *   true iff the timestamps are in chronological order
    */
  final def validateTimestampOrder(first: Timestamp, second: Timestamp): Boolean = first <= second

  // Same as validateTimestampOrder except that it returns a GraphMessage
  def checkTimestampOrder(
      rpcMessage: JsonRpcRequest,
      first: Timestamp,
      second: Timestamp,
      error: PipelineError
  ): GraphMessage = {
    if (validateTimestampOrder(first, second))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** This method behaves the same as checkTimestampOrder, except that the param <second> is not necessarily defined. (Wrt to the protocol, some fields are not necessarily defined for certain type of messages, such as CreateMeeting)
    */
  def checkOptionalTimestampOrder(rpcMessage: JsonRpcRequest, first: Timestamp, second: Option[Timestamp], error: PipelineError): GraphMessage = {
    if (!second.isDefined)
      Right(rpcMessage)
    else
      checkTimestampOrder(rpcMessage, first, second.get, error)
  }

  /** Checks if the id corresponds to the expected id
    *
    * @param rpcMessage
    *   rpc message to check
    * @param expectedId
    *   expected id
    * @param id
    *   id to check
    * @param error
    *   the error to forward in case the id is not the same as the expected id
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  def checkId(rpcMessage: JsonRpcRequest, expectedId: Hash, id: Hash, error: PipelineError): GraphMessage = {
    if (expectedId == id)
      Right(rpcMessage)
    else
      Left(error)
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
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  def checkWitnesses(rpcMessage: JsonRpcRequest, witnesses: List[PublicKey], error: PipelineError): GraphMessage = {
    if (validateWitnesses(witnesses))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Check if some String match the pattern
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param str
    *   the string to check
    * @param pattern
    *   the pattern the name must match
    * @param error
    *   the error to forward in case the name doesn't match the pattern
    * @return
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  def checkStringPattern(rpcMessage: JsonRpcRequest, str: String, pattern: Regex, error: PipelineError): GraphMessage = {
    if (pattern.matches(str))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Check if some message id exist in the db, if option id is empty the check is successful
    *
    * @param rpcMessage
    *   the rpc message to validate
    * @param id
    *   the Option message id to check existence for
    * @param channel
    *   the channel on which the message might exist
    * @param dbActor
    *   the dbActor to ask
    * @param error
    *   the error to throw if the chirp do not exist
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful Left with pipeline error
    */
  def checkIdExistence(rpcMessage: JsonRpcRequest, id: Option[Hash], channel: Channel, dbActor: AskableActorRef, error: PipelineError): GraphMessage = {
    id match {
      case Some(id) =>
        val ask = dbActor ? Read(channel, id)
        Await.ready(ask, duration).value.get match {
          // just care about the message id existence
          case Success(_) => Right(rpcMessage)
          case _          => Left(error)
        }

      case None => Right(rpcMessage)
    }
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
    *   GraphMessage: passes the rpcMessages to Right if successful Left with pipeline error
    */
  def checkWitnessesSignatures(
      rpcMessage: JsonRpcRequest,
      witnessesKeyPairs: List[WitnessSignaturePair],
      id: Hash,
      error: PipelineError
  ): GraphMessage = {
    if (validateWitnessSignatures(witnessesKeyPairs, id))
      Right(rpcMessage)
    else
      Left(error)
  }

  /** Check whether a list of <witnesses> public keys are valid or not
    *
    * @param witnesses
    *   list of witnesses public keys
    * @return
    *   true iff the public keys are all distinct
    */
  final def validateWitnesses(witnesses: List[PublicKey]): Boolean = witnesses.size == witnesses.toSet.size

  /** Check whether a list of <witnessesKeyPairs> are valid modification_id <data>
    *
    * @param witnessesKeyPairs
    *   list of witness key-signature pairs
    * @param data
    *   modification_id of the message
    * @return
    *   true iff the witness key-signature pairs are valid wrt. modification_id data
    */
  final def validateWitnessSignatures(witnessesKeyPairs: List[WitnessSignaturePair], data: Hash): Boolean =
    witnessesKeyPairs.forall(wsp => wsp.verify(data))

}
