package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}

import scala.io.{BufferedSource, Source}
import ch.epfl.pop.model.network.method.message.data.cash.PostTransaction

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

    val expected = CreateLao(Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")), "LAO", Timestamp(1633098234L), PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")), Nil)

    messageData shouldBe a[CreateLao]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a SetupElection message data") {
    val example: String = getExampleMessage("messageData/election_setup/election_setup.json")
    val messageData = SetupElection.buildFromJson(example)

    val question = ElectionQuestion(Hash(Base64Data("2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=")), "Is this project fun?", "Plurality", "Yes" :: "No" :: Nil, write_in = false)
    val expected = SetupElection(Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=")), Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")), "Election", "1.0.0", Timestamp(1633098941L), Timestamp(1633098941L), Timestamp(1633099812L), question :: Nil)

    messageData shouldBe a[SetupElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a ResultElection message data") {
    val example: String = getExampleMessage("messageData/election_result.json")

    val messageData = ResultElection.buildFromJson(example)

    val result1 = ElectionBallotVotes("Yes", 1)
    val result2 = ElectionBallotVotes("No", 0)
    val questionResult = ElectionQuestionResult(Hash(Base64Data("2PLwVvqxMqW5hQJXkFpNCvBI9MZwuN8rf66V1hS-iZU=")), result1 :: result2 :: Nil)
    val expected = ResultElection(questionResult :: Nil, Nil)

    messageData shouldBe a[ResultElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a EndElection message data") {
    val example: String = getExampleMessage("messageData/election_end/election_end.json")
    val messageData = EndElection.buildFromJson(example)

    val expected = EndElection(Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")), Hash(Base64Data("zG1olgFZwA0m3mLyUqeOqrG0MbjtfqShkyZ6hlyx1tg=")), Timestamp(1633099883L), Hash(Base64Data("tAUYpZDc7lOfrxyviK6V9UsezeubGUZR-TpwF52pzWU=")))

    messageData shouldBe a[EndElection]
    messageData should equal(expected)
  }

  test("Parser correctly decodes a CastVoteElection write_in message data") {
    val example: String = getExampleMessage("messageData/vote_cast_write_in.json")
    val messageData = CastVoteElection.buildFromJson(example)

    val votes = VoteElection(Hash(Base64Data("DtIsj7nQ0Y4iLJ4ETKv2D0uah7IYGyEVW7aCLFjaL0w=")), Hash(Base64Data("WBVsWJI-C5YkD0wdE4DxnLa0lJzjnHEd67XPFVB9v3g=")), "Computer Science")
    val expected = CastVoteElection(Hash(Base64Data("fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=")), Hash(Base64Data("QWTmcWMMMiUdWdZX7ib7GyqH6A5ifDYwPaMpKxIZm1k=")), Timestamp(1633098996L), votes :: Nil)

    messageData shouldBe a[CastVoteElection]
    messageData shouldEqualTo (expected)
  }

  test("Parser correctly encodes/decodes a CashTransaction message data") {
    val example: String = getExampleMessage("messageData/cash/post_transaction.json")
    val messageData = PostTransaction.buildFromJson(example)

    val expected = PostTransaction(Transaction(Version=1, TxIn=List(TxIn(Hash(Base64Data("47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=")), 0, UnlockScript("P2PKH", PublicKey(Base64Data("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")), Base64Data("CAFEBABE")))), TxOut=List(TxOut(32, LockScript("P2PKH",Address(Base64Data("2jmj7l5rSw0yVb-vlWAYkK-YBwk=")))))))

    messageData shouldBe a[PostTransaction]
    messageData should equal (expected)
  }
}
