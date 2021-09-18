package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, ElectionBallotVotes, ElectionQuestion, ElectionQuestionResult, EndElection, ResultElection, SetupElection, VoteElection}
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}

import scala.io.{BufferedSource, Source}

class MessageDataProtocolSuite extends FunSuite with Matchers {

  final val EXAMPLES_DIRECTORY_PATH: String = "../protocol/examples"

  implicit class RichCastVoteElection(cv: CastVoteElection) {
    def shouldEqualTo(o: CastVoteElection): Unit = {

      @scala.annotation.tailrec
      def checkVoteElectionLists(l1: List[VoteElection], l2: List[VoteElection]) {
        l1.length should equal (l2.length)

        (l1, l2) match {
          case _ if l1.isEmpty =>
          case (h1 :: tail1, h2 :: tail2) =>
            h1.id should equal (h2.id)
            h1.question should equal (h2.question)
            h1.vote should equal (h2.vote)
            h1.write_in should equal (h2.write_in)

            checkVoteElectionLists(tail1, tail2)
        }
      }

      val cv_1: CastVoteElection = this.cv
      val cv_2: CastVoteElection = o

      cv_1 shouldBe a [CastVoteElection]
      cv_2 shouldBe a [CastVoteElection]

      cv_1.lao should equal (cv_2.lao)
      cv_1.election should equal (cv_2.election)
      cv_1.created_at should equal (cv_2.created_at)
      println(cv_1.votes.head)
      println(cv_2.votes.head)
      checkVoteElectionLists(cv_1.votes, cv_2.votes)
    }
  }
  

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
    messageData shouldEqualTo (expected)
  }
}
