package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.data.election.ElectionQuestion
import ch.epfl.pop.model.objects.{Hash, PublicKey, Timestamp, WitnessSignaturePair}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, PipelineError}
import ch.epfl.pop.storage.DbActor

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
