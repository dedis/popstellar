package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.Hash
import spray.json._

class VoteElection(val id: Hash, val question: Hash, val vote: Option[Int], val write_in: Option[String]) {

  def this(id: Hash, question: Hash, vote: Int) = this(id, question, Some(vote), None)

  def this(id: Hash, question: Hash, write_in: String) = this(id, question, None, Some(write_in))

  def isWriteIn: Boolean = write_in.isDefined
}

object VoteElection extends Parsable {
  def apply(id: Hash, question: Hash, vote: Int): VoteElection = new VoteElection(id, question, Some(vote), None)

  def apply(id: Hash, question: Hash, writeIn: String): VoteElection = new VoteElection(id, question, None, Some(writeIn))

  def apply(
             id: Hash,
             question: Hash,
             vote: Option[Int],
             write_in: Option[String]
           ): VoteElection = new VoteElection(id, question, vote, write_in)

  override def buildFromJson(payload: String): VoteElection = payload.parseJson.asJsObject.convertTo[VoteElection]
}
