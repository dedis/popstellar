package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, ElectionBallotVotes, ElectionQuestion, ElectionQuestionResult, EndElection, ResultElection, SetupElection, VoteElection}
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}

import scala.io.{BufferedSource, Source}

class MessageDataProtocolSuite extends FunSuite with Matchers {

  final val EXAMPLES_DIRECTORY_PATH: String = "../protocol/examples"

  // path is the path with protocol/examples as base directory
  private def getExampleMessage(path: String): String = {
    val bufferedSource: BufferedSource = Source.fromFile(s"$EXAMPLES_DIRECTORY_PATH/$path")
    val example: String = bufferedSource.getLines().mkString
    bufferedSource.close()

    example
  }

  test("Parser correctly encodes/decodes a CreateLao message data") {
    val example: String = getExampleMessage("messageData/lao_create.json")
    val messageData = CreateLao.buildFromJson(example)

    val expected = CreateLao(Hash(Base64Data("XXX")), "XXX", Timestamp(123L), PublicKey(Base64Data("XXX")), PublicKey(Base64Data("XXX")) :: Nil)

    messageData shouldBe a [CreateLao]
    messageData should equal (expected)
  }

  test("Parser correctly encodes/decodes a SetupElection message data") {
    val example: String = getExampleMessage("messageData/election_setup.json")
    val messageData = SetupElection.buildFromJson(example)

    val question = ElectionQuestion(Hash(Base64Data("XXX")), "XXX", "Plurality", "XXX" :: "YYY" :: Nil, write_in = false)
    val expected = SetupElection(Hash(Base64Data("XXX")), Hash(Base64Data("XXX")), "XXX", "XXX", Timestamp(123L), Timestamp(123L), Timestamp(123L), question :: Nil)

    messageData shouldBe a [SetupElection]
    messageData should equal (expected)
  }

  test("Parser correctly encodes/decodes a ResultElection message data") {
    val example: String = getExampleMessage("messageData/election_result.json")

    val messageData = ResultElection.buildFromJson(example)

    val result = ElectionBallotVotes("XXX", 123)
    val questionResult = ElectionQuestionResult(Hash(Base64Data("XXX")), result :: Nil)
    val expected = ResultElection(questionResult :: Nil, Signature(Base64Data("XXX")) :: Nil)

    messageData shouldBe a [ResultElection]
    messageData should equal (expected)
  }

  test("Parser correctly encodes/decodes a EndElection message data") {
    val example: String = getExampleMessage("messageData/election_end.json")
    val messageData = EndElection.buildFromJson(example)

    val expected = EndElection(Hash(Base64Data("XXX")), Hash(Base64Data("XXX")), Timestamp(123L), Hash(Base64Data("XXX")))

    messageData shouldBe a [EndElection]
    messageData should equal (expected)
  }

  test("Parser correctly encodes/decodes a CastVoteElection write_in message data") {
    val example: String = getExampleMessage("messageData/vote_cast_write_in.json")
    val messageData = CastVoteElection.buildFromJson(example)

    val votes = VoteElection(Hash(Base64Data("XXX")), Hash(Base64Data("XXX")), "XXX")
    val expected = CastVoteElection(Hash(Base64Data("XXX")), Hash(Base64Data("XXX")), Timestamp(123L), votes :: Nil)

    messageData shouldBe a [CastVoteElection]
    messageData should equal (expected)
  }
}
