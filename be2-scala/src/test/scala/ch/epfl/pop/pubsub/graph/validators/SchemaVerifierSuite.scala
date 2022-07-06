package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.pubsub.MessageRegistry
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.pubsub.graph.SchemaVerifier._
import org.scalatest.{FunSuite, Matchers}

import scala.io.Source
import scala.util.{Failure, Success}

object SchemaVerifierSuite {
  def getJsonStringFromFile(filePath: String): String = {
    val source = Source.fromFile(filePath)
    val jsonStr = source.getLines().mkString
    source.close
    jsonStr
  }
}

class SchemaVerifierSuite extends FunSuite with Matchers {

  import SchemaVerifierSuite._

  /* ----------------------------- Test invalid JSON requests ----------------------------- */
  test("Invalid jsonString - string instead of JSON object") {
    val invalidJSON = "wrond JSON string"

    verifyRpcSchema(invalidJSON) shouldBe a[Right[_, PipelineError]]
  }

  /* ----------------------------- High-level (JSON-rpc) tests ----------------------------- */
  test("Correct subscribe JSON-RPC query") {
    val subscribePath = "../protocol/examples/query/subscribe/subscribe.json"
    val subscribeJson = getJsonStringFromFile(subscribePath)

    verifyRpcSchema(subscribeJson) should be(Left(subscribeJson))
  }

  test("Incorrect subscribe query: additional params") {
    val subscribePath = "../protocol/examples/query/subscribe/wrong_subscribe__additional_params.json"
    val subscribeJson = getJsonStringFromFile(subscribePath)

    verifyRpcSchema(subscribeJson) shouldBe a[Right[_, PipelineError]]
  }

  test("Incorrect subscribe query: missing channel") {
    val subscribePath = "../protocol/examples/query/subscribe/wrong_subscribe_missing_channel.json"
    val subscribeJson = getJsonStringFromFile(subscribePath)

    verifyRpcSchema(subscribeJson) shouldBe a[Right[_, PipelineError]]
  }

  /* Test broadcast query with message format */
  test("Correct broadcast JSON-RPC query") {
    val broadcastPath = "../protocol/examples/query/broadcast/broadcast.json"
    val broadcastJson = getJsonStringFromFile(broadcastPath)

    verifyRpcSchema(broadcastJson) should be(Left(broadcastJson))
  }

  test("Incorrect broadcast query: additional params") {
    val broadcastPath = "../protocol/examples/query/broadcast/wrong_broadcast_additional_params.json"
    val broadcastJson = getJsonStringFromFile(broadcastPath)

    verifyRpcSchema(broadcastJson) shouldBe a[Right[_, PipelineError]]
  }

  test("Incorrect broadcast query: missing message") {
    val broadcastPath = "../protocol/examples/query/broadcast/wrong_broadcast_missing_message.json"
    val broadcastJson = getJsonStringFromFile(broadcastPath)

    verifyRpcSchema(broadcastJson) shouldBe a[Right[_, PipelineError]]
  }

  test("Correct unsubscribe JSON-RPC query") {
    val unsubscribePath = "../protocol/examples/query/unsubscribe/unsubscribe.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    verifyRpcSchema(unsubscribeJson) should be(Left(unsubscribeJson))
  }

  test("Incorrect unsubscribe query: additional params") {
    val unsubscribePath = "../protocol/examples/query/unsubscribe/wrong_unsubscribe__additional_params.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    verifyRpcSchema(unsubscribeJson) shouldBe a[Right[_, PipelineError]]
  }

  test("Incorrect unsubscribe query: missing channel") {
    val unsubscribePath = "../protocol/examples/query/unsubscribe/wrong_unsubscribe_missing_channel.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    verifyRpcSchema(unsubscribeJson) shouldBe a[Right[_, PipelineError]]
  }

  test("Incorrect unsubscribe query: wrong channel") {
    val unsubscribePath = "../protocol/examples/query/unsubscribe/wrong_unsubscribe_channel.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    verifyRpcSchema(unsubscribeJson) shouldBe a[Right[_, PipelineError]]
  }

  /* ----------------------------- Low-level (data) tests ----------------------------- */
  test("Correct CreateLao data") {
    val examplePath = "../protocol/examples/messageData/lao_create/lao_create.json"
    val exampleJson = getJsonStringFromFile(examplePath)
    val validator = MessageRegistry.apply().getSchemaVerifier(ObjectType.LAO, ActionType.CREATE).get

    validator(exampleJson) should be(Success((): Unit))
  }

  test("Incorrect CreateLao data: negative timestamp") {
    val examplePath = "../protocol/examples/messageData/lao_create/bad_lao_create_creation_negative.json"
    val exampleJson = getJsonStringFromFile(examplePath)
    val validator = MessageRegistry.apply().getSchemaVerifier(ObjectType.LAO, ActionType.CREATE).get

    validator(exampleJson) shouldBe a[Failure[_]]
  }

  test("Correct CoinTransaction data") {
    val examplePath = "../protocol/examples/messageData/coin/post_transaction.json"
    val exampleJson = getJsonStringFromFile(examplePath)
    val validator = MessageRegistry.apply().getSchemaVerifier(ObjectType.COIN, ActionType.POST_TRANSACTION).get

    validator(exampleJson) should be(Success((): Unit))
  }

  test("Incorrect CashTransaction data: overflow amount") {
    val examplePath = "../protocol/examples/messageData/coin/post_transaction_overflow_amount.json"
    val exampleJson = getJsonStringFromFile(examplePath)
    val validator = MessageRegistry.apply().getSchemaVerifier(ObjectType.COIN, ActionType.POST_TRANSACTION).get

    validator(exampleJson) shouldBe a[Failure[_]]
  }

  test("Incorrect CashTransaction data: negative amount") {
    val examplePath = "../protocol/examples/messageData/coin/post_transaction_negative_amount.json"
    val exampleJson = getJsonStringFromFile(examplePath)
    val validator = MessageRegistry.apply().getSchemaVerifier(ObjectType.COIN, ActionType.POST_TRANSACTION).get

    validator(exampleJson) shouldBe a[Failure[_]]
  }
}
