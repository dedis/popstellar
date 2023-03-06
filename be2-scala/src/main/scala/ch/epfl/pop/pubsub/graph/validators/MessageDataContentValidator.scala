package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp, WitnessSignaturePair}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

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
      Left(rpcMessage)
    else
      Right(error)
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
      Left(rpcMessage)
    else
      Right(error)
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
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  def checkId(rpcMessage: JsonRpcRequest, expectedId: Hash, id: Hash, error: PipelineError): GraphMessage = {
    if (expectedId == id)
      Left(rpcMessage)
    else
      Right(error)
  }

  /** Check if all witnesses are distinct
   *
   * @param rpcMessage
   * rpc message to validate
   * @param witnesses
   * witnesses to check
   * @param error
   * the error to forward in case the witnesses are not all distinct
   * @return
   * GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
   */
  def checkWitnesses(rpcMessage: JsonRpcRequest, witnesses: List[PublicKey], error: PipelineError): GraphMessage = {
    if (validateWitnesses(witnesses))
      Left(rpcMessage)
    else
      Right(error)
  }

  /** Check if some String match the pattern
   *
   * @param rpcMessage
   * rpc message to validate
   * @param str
   * the string to check
   * @param pattern
   * the pattern the name must match
   * @param error
   * the error to forward in case the name doesn't match the pattern
   * @return
   * GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
   */
  def checkStringPattern(rpcMessage: JsonRpcRequest, str: String, pattern: Regex, error: PipelineError): GraphMessage = {
    if (pattern.matches(str))
      Left(rpcMessage)
    else
      Right(error)
  }

  /** Checks witnesses key signature pairs for given modification id
   *
   * @param rpcMessage
   * rpc message to validate
   * @param witnessesKeyPairs
   * the witness key signature pairs
   * @param id
   * modification id of the message
   * @param error
   * the error to forward in case of invalid modifications
   * @return
   * GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
   */
  def checkWitnessesSignatures(rpcMessage: JsonRpcRequest, witnessesKeyPairs: List[WitnessSignaturePair], id: Hash, error: PipelineError): GraphMessage = {
    if (validateWitnessSignatures(witnessesKeyPairs, id))
      Left(rpcMessage)
    else
      Right(error)
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
