package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.data.election.*
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao}
import ch.epfl.pop.model.objects.*
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers

import scala.io.{BufferedSource, Source}
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction
import ch.epfl.pop.model.network.method.message.data.election.VersionType.*
import ch.epfl.pop.model.network.method.message.data.federation.{FederationChallenge, FederationChallengeRequest, FederationExpect, FederationInit, FederationResult}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import spray.json.*
import util.examples.Federation.FederationChallengeExample.{CHALLENGE, CHALLENGE_1}
import util.examples.Federation.FederationChallengeRequestExample.CHALLENGE_REQUEST
import util.examples.Federation.FederationExpectExample.{EXPECT, EXPECT_1}
import util.examples.Federation.FederationInitExample.{INIT, INIT_1}
import util.examples.Federation.FederationResultExample.{RESULT_1, RESULT_1_1, RESULT_2, RESULT_2_2}

class MessageDataProtocolSuite extends FunSuite with Matchers {

  final val EXAMPLES_DIRECTORY_PATH: String = "../protocol/examples"

  implicit class RichCastVoteElection(cv: CastVoteElection) {
    def shouldEqualTo(o: CastVoteElection): Unit = {

      @scala.annotation.tailrec
      def checkVoteElectionLists(l1: List[VoteElection], l2: List[VoteElection]): Unit = {
        l1.length should equal(l2.length)

        (l1, l2) match {
          case (Nil, _) | (_, Nil) =>
          case (h1 :: tail1, h2 :: tail2) =>
            h1.id should equal(h2.id)
            h1.question should equal(h2.question)
            h1.vote should equal(h2.vote)
            h1.write_in should equal(h2.write_in)

            checkVoteElectionLists(tail1, tail2)
        }
      }

      val cv_1: CastVoteElection = this.cv
      val cv_2: CastVoteElection = o

      cv_1 shouldBe a[CastVoteElection]
      cv_2 shouldBe a[CastVoteElection]

      cv_1.lao should equal(cv_2.lao)
      cv_1.election should equal(cv_2.election)
      cv_1.created_at should equal(cv_2.created_at)
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

  test("Parser correctly decodes a CreateLao message data") {
    val example: String = getExampleMessage("messageData/lao_create/lao_create.json")
    val messageData = CreateLao.buildFromJson(example)

    val expected = CreateLao(
      Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")),
      "LAO",
      Timestamp(1633098234L),
      PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
      Nil
    )

    messageData shouldBe a[CreateLao]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a SetupElection message data") {
    val example: String = getExampleMessage("messageData/election_setup/election_setup.json")
    val messageData = SetupElection.buildFromJson(example)

    val question = ElectionQuestion(Hash(Base64Data("2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=")), "Is this project fun?", "Plurality", "Yes" :: "No" :: Nil, write_in = false)
    val expected = SetupElection(
      Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=")),
      Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")),
      "Election",
      OPEN_BALLOT,
      Timestamp(1633098941L),
      Timestamp(1633098941L),
      Timestamp(1633099812L),
      question :: Nil
    )

    messageData shouldBe a[SetupElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a OpenElection message data") {
    val example: String = getExampleMessage("messageData/election_open/election_open.json")

    val messageData = OpenElection.buildFromJson(example)

    val lao = Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="))
    val election = Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg="))
    val opened_at = Timestamp(1633099883L)
    val expected = OpenElection(lao, election, opened_at)

    messageData shouldBe a[OpenElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a EndElection message data") {
    val example: String = getExampleMessage("messageData/election_end/election_end.json")
    val messageData = EndElection.buildFromJson(example)

    val expected = EndElection(
      Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")),
      Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=")),
      Timestamp(1633099883L),
      Hash(Base64Data("GX9slST3yY_Mltkjimp-eNq71mfbSbQ9sruABYN8EoM="))
    )

    messageData shouldBe a[EndElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a CastVoteElection message data") {
    val example: String = getExampleMessage("messageData/vote_cast_vote/vote_cast_vote.json")
    val messageData = CastVoteElection.buildFromJson(example)

    val votes = VoteElection(Hash(Base64Data("8L2MWJJYNGG57ZOKdbmhHD9AopvBaBN26y1w5jL07ms=")), Hash(Base64Data("2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=")), 0)
    val expected = CastVoteElection(
      Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")),
      Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=")),
      Timestamp(1633098941L),
      votes :: Nil
    )

    messageData shouldBe a[CastVoteElection]
    messageData shouldEqualTo expected
  }

  test("Parser correctly decodes a KeyElection message data") {
    val example: String = getExampleMessage("messageData/election_key/election_key.json")
    val messageData = KeyElection.buildFromJson(example)

    val expected = KeyElection(Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=")), PublicKey(Base64Data("JsS0bXJU8yMT9jvIeTfoS6RJPZ8YopuAUPkxssHaoTQ")))

    messageData shouldBe a[KeyElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a ResultElection write_in message data") {
    val example: String = getExampleMessage("messageData/election_result.json")
    val messageData = ResultElection.buildFromJson(example)

    val result = List(ElectionQuestionResult(Hash(Base64Data("2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=")), List(ElectionBallotVotes("Yes", 1), ElectionBallotVotes("No", 0))))
    val expected = ResultElection(result, List.empty)

    messageData shouldBe a[ResultElection]
    messageData should equal(expected)
  }

  test("Parser correctly encodes/decodes a PostTransaction message data") {
    val example: String = getExampleMessage("messageData/coin/post_transaction.json")
    val messageData = PostTransaction.buildFromJson(example)

    val expected = PostTransaction(
      Transaction(
        version = 1,
        inputs = List(Transaction.Input(
          Hash(Base64Data("01N1Y8twdu7wpdz5HLnkIeQSeuKpkNcQHeKF7XabLYU=")),
          0,
          UnlockScript(
            "P2PKH",
            PublicKey(Base64Data("oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=")),
            Base64Data("6hY16bDKnb7bRA5j7IMDR1CJDqAJKOLuQRgKxdpYQIrtSTVTRjo5jigqPhYQEPcK5a1WAF86V739ENFnlp6YCw==")
          )
        )),
        outputs = List(Transaction.Output(32, LockScript("P2PKH", Address(Base64Data("-_qR4IHwsiq50raa8jURNArds54="))))),
        lockTime = 0
      ),
      transactionId = Hash(Base64Data("ESQimXKtIrt85EMSsmGoQ2kE2y1-ae6SAvp52Xp3KOQ="))
    )

    messageData shouldBe a[PostTransaction]
    messageData should equal(expected)
  }

  test("Parser correctly encodes/decodes a CashTransaction message data with the max amount") {
    val example: String = getExampleMessage("messageData/coin/post_transaction_max_amount.json")
    val messageData = PostTransaction.buildFromJson(example)

    val expected = PostTransaction(
      Transaction(
        version = 1,
        inputs = List(Transaction.Input(
          Hash(Base64Data("01N1Y8twdu7wpdz5HLnkIeQSeuKpkNcQHeKF7XabLYU=")),
          0,
          UnlockScript(
            "P2PKH",
            PublicKey(Base64Data("oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=")),
            Base64Data("Egx9igwYeDRqpi3mVHFE-kEkpttjMgdKgfIl-umnr3DsH7RtZOAXULUnZFjkQijJNLx2tBPgMrIjnVTIh6bQAg==")
          )
        )),
        outputs = List(Transaction.Output((1L << 53) - 1, LockScript("P2PKH", Address(Base64Data("2jmj7l5rSw0yVb-vlWAYkK-YBwk="))))),
        lockTime = 0
      ),
      transactionId = Hash(Base64Data("tckbNAMUVbkGX_v3Wy8d2XQjzf0q6MG72KL2cExCmHc="))
    )

    messageData shouldBe a[PostTransaction]
    messageData should equal(expected)
  }

  test("Parser correctly encodes and decode GreetLao") {
    val expectedGreetLao = GreetLao(
      Hash(Base64Data("p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=")),
      PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
      "wss://popdemo.dedis.ch:8000/demo",
      List("wss://popdemo.dedis.ch:8000/second-organizer-demo", "wss://popdemo.dedis.ch:8000/witness-demo")
    )

    val example = getExampleMessage("messageData/lao_greet/greeting.json")

    val greetLaoFromExample = GreetLao.buildFromJson(example)
    val buildGreetJson = MessageDataProtocol.GreetLaoFormat.write(expectedGreetLao)
    val greetLaoFromBuiltJson = MessageDataProtocol.GreetLaoFormat.read(buildGreetJson)

    greetLaoFromBuiltJson shouldBe a[GreetLao]
    greetLaoFromExample shouldBe a[GreetLao]
    greetLaoFromExample should equal(expectedGreetLao)
    greetLaoFromBuiltJson should equal(expectedGreetLao)
  }

  test("Parser correctly encodes and decodes FederationChallenge") {
    val example = getExampleMessage("messageData/federation_challenge/federation_challenge.json")
    val expectedFederationChallenge = CHALLENGE_1

    val federationChallengeFromExample = FederationChallenge.buildFromJson(example)
    val buildChallengeJson = MessageDataProtocol.FederationChallengeFormat.write(expectedFederationChallenge)
    val federationChallengeFromBuiltJson = MessageDataProtocol.FederationChallengeFormat.read(buildChallengeJson)

    federationChallengeFromBuiltJson shouldBe a[FederationChallenge]
    federationChallengeFromExample shouldBe a[FederationChallenge]
    federationChallengeFromExample should equal(expectedFederationChallenge)
    federationChallengeFromBuiltJson should equal(expectedFederationChallenge)
  }

  test("Parser correctly encodes and decodes FederationChallengeRequest") {
    val example = getExampleMessage("messageData/federation_challenge_request/federation_challenge_request.json")
    val expectedFederationChallengeRequest = CHALLENGE_REQUEST

    val federationChallengeRequestFromExample = FederationChallengeRequest.buildFromJson(example)
    val buildChallengeRequestJson = MessageDataProtocol.FederationChallengeRequestFormat.write(expectedFederationChallengeRequest)
    val federationChallengeRequestFromBuiltJson = MessageDataProtocol.FederationChallengeRequestFormat.read(buildChallengeRequestJson)

    federationChallengeRequestFromBuiltJson shouldBe a[FederationChallengeRequest]
    federationChallengeRequestFromExample shouldBe a[FederationChallengeRequest]
    federationChallengeRequestFromExample should equal(expectedFederationChallengeRequest)
    federationChallengeRequestFromBuiltJson should equal(expectedFederationChallengeRequest)
  }

  test("Parser correctly encodes and decodes FederationInit") {
    val example = getExampleMessage("messageData/federation_init/federation_init.json")
    val expectedFederationInit = INIT_1

    val federationInitFromExample = FederationInit.buildFromJson(example)
    val buildInitJson = MessageDataProtocol.FederationInitFormat.write(expectedFederationInit)
    val federationInitFromBuiltJson = MessageDataProtocol.FederationInitFormat.read(buildInitJson)

    federationInitFromBuiltJson shouldBe a[FederationInit]
    federationInitFromExample shouldBe a[FederationInit]
    federationInitFromExample should equal(expectedFederationInit)
    federationInitFromBuiltJson should equal(expectedFederationInit)

  }

  test("Parser correctly encodes and decodes FederationExpect") {
    val example = getExampleMessage("messageData/federation_expect/federation_expect.json")
    val expectedFederationExpect = EXPECT_1

    val federationExpectFromExample = FederationExpect.buildFromJson(example)
    val buildExpectJson = MessageDataProtocol.FederationExpectFormat.write(expectedFederationExpect)
    val federationExpectFromBuiltJson = MessageDataProtocol.FederationExpectFormat.read(buildExpectJson)

    federationExpectFromBuiltJson shouldBe a[FederationExpect]
    federationExpectFromExample shouldBe a[FederationExpect]
    federationExpectFromExample should equal(expectedFederationExpect)
    federationExpectFromBuiltJson should equal(expectedFederationExpect)

  }

  test("Parser correctly encodes and decodes FederationResult") {
    val example_1 = getExampleMessage("messageData/federation_result/federation_result.json")
    val example_2 = getExampleMessage("messageData/federation_result/federation_result_2.json")
    val expectedFederationResult_1 = RESULT_1_1
    val expectedFederationResult_2 = RESULT_2_2

    val federationResultFromExample_1 = FederationResult.buildFromJson(example_1)
    val buildResultJson_1 = MessageDataProtocol.FederationResultFormat.write(expectedFederationResult_1)
    val federationResultFromBuiltJson_1 = MessageDataProtocol.FederationResultFormat.read(buildResultJson_1)

    val federationResultFromExample_2 = FederationResult.buildFromJson(example_2)
    val buildResultJson_2 = MessageDataProtocol.FederationResultFormat.write(expectedFederationResult_2)
    val federationResultFromBuiltJson_2 = MessageDataProtocol.FederationResultFormat.read(buildResultJson_2)

    federationResultFromBuiltJson_1 shouldBe a[FederationResult]
    federationResultFromExample_1 shouldBe a[FederationResult]
    federationResultFromExample_1 should equal(expectedFederationResult_1)
    federationResultFromBuiltJson_1 should equal(expectedFederationResult_1)

    federationResultFromBuiltJson_2 shouldBe a[FederationResult]
    federationResultFromExample_2 shouldBe a[FederationResult]
    federationResultFromExample_2 should equal(expectedFederationResult_2)
    federationResultFromBuiltJson_2 should equal(expectedFederationResult_2)
  }

  test("Parser correctly encodes and decodes ObjectType") {
    ObjectType.values.foreach(obj => {
      val fromJson = MessageDataProtocol.objectTypeFormat.write(obj)
      if obj != ObjectType.INVALID then {
        val toType = MessageDataProtocol.objectTypeFormat.read(fromJson)
        toType shouldBe a[ObjectType]
      }
    })
  }

  test("Parser correctly rejects incorrect ObjectType") {
    val fromJson = MessageDataProtocol.objectTypeFormat.write(ObjectType.INVALID)
    assertThrows[IllegalArgumentException] {
      MessageDataProtocol.objectTypeFormat.read(fromJson)
    }

    val invalidJson = """{"object": "stellarobject"}""".parseJson
    assertThrows[IllegalArgumentException] {
      MessageDataProtocol.objectTypeFormat.read(invalidJson)
    }
  }

  test("Parser correctly encodes and decodes ActionType") {
    ActionType.values.foreach(obj => {
      val fromJson = MessageDataProtocol.actionTypeFormat.write(obj)
      if obj != ActionType.INVALID then {
        val toType = MessageDataProtocol.actionTypeFormat.read(fromJson)
        toType shouldBe a[ActionType]
      }
    })
  }

  test("Parser correctly rejects incorrect ActionType") {
    val fromJson = MessageDataProtocol.actionTypeFormat.write(ActionType.INVALID)
    assertThrows[IllegalArgumentException] {
      MessageDataProtocol.actionTypeFormat.read(fromJson)
    }

    val invalidJson = """{"object": "stellaraction"}""".parseJson
    assertThrows[IllegalArgumentException] {
      MessageDataProtocol.actionTypeFormat.read(invalidJson)
    }
  }

  test("Parser correctly encodes and decodes VersionType") {
    VersionType.values.foreach(obj => {
      val fromJson = MessageDataProtocol.versionTypeFormat.write(obj)
      if obj != VersionType.INVALID then {
        val toType = MessageDataProtocol.versionTypeFormat.read(fromJson)
        toType shouldBe a[VersionType]
      }
    })
  }

  test("Parser correctly rejects incorrect VersionType") {
    val fromJson = MessageDataProtocol.versionTypeFormat.write(VersionType.INVALID)
    assertThrows[IllegalArgumentException] {
      MessageDataProtocol.versionTypeFormat.read(fromJson)
    }

    val invalidJson = """{"object": "stellarversion"}""".parseJson
    assertThrows[IllegalArgumentException] {
      MessageDataProtocol.versionTypeFormat.read(invalidJson)
    }
  }

}
