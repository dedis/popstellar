package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.pubsub.graph.Validator._
import org.scalatest.{FunSuite, Matchers}

import scala.io.Source

object SchemaValidatorSuite {
    def getJsonStringFromFile(filePath: String): String = {
      val source = Source.fromFile(filePath);
      val jsonStr = source.getLines.mkString
      source.close
      jsonStr
    }
}
class SchemaValidatorSuite extends FunSuite with Matchers {
   import SchemaValidatorSuite._

  /* Test subscribe query without message format*/
  test("Correct subscribe JSON-RPC query"){
    val subscribePath = "../protocol/examples/query/subscribe/subscribe.json"
    val subscribeJson = getJsonStringFromFile(subscribePath)

    validateSchema(subscribeJson) should be (Left(subscribeJson))
  }

  test("Incorrect subscribe query: additional params"){
    val subscribePath = "../protocol/examples/query/subscribe/wrong_subscribe__additional_params.json"
    val subscribeJson = getJsonStringFromFile(subscribePath)

    validateSchema(subscribeJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Incorrect subscribe query: missing channel"){
    val subscribePath = "../protocol/examples/query/subscribe/wrong_subscribe_missing_channel.json"
    val subscribeJson = getJsonStringFromFile(subscribePath)

    validateSchema(subscribeJson) shouldBe a [Right[_,PipelineError]]
  }

  /* Test broadcast query with message format */
  test("Correct broadcast JSON-RPC query"){
    val broadcastPath = "../protocol/examples/query/broadcast/broadcast.json"
    val broadcastJson = getJsonStringFromFile(broadcastPath)

    validateSchema(broadcastJson) should be (Left(broadcastJson))
  }

  test("Incorrect broadcast query: additional params"){
    val broadcastPath = "../protocol/examples/query/broadcast/wrong_broadcast_additional_params.json"
    val broadcastJson =getJsonStringFromFile(broadcastPath)

    validateSchema(broadcastJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Incorrect broadcast query: missing message"){
    val broadcastPath = "../protocol/examples/query/broadcast/wrong_broadcast_missing_message.json"
    val broadcastJson = getJsonStringFromFile(broadcastPath)

    validateSchema(broadcastJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Correct unsubscribe JSON-RPC query"){
    val unsubscribePath = "../protocol/examples/query/unsubscribe/unsubscribe.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    validateSchema(unsubscribeJson) should be (Left(unsubscribeJson))
  }

  test("Incorrect unsubscribe query: additional params"){
    val unsubscribePath = "../protocol/examples/query/unsubscribe/wrong_unsubscribe__additional_params.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    validateSchema(unsubscribeJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Incorrect unsubscribe query: missing channel"){
    val unsubscribePath = "../protocol/examples/query/unsubscribe/wrong_unsubscribe_missing_channel.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    validateSchema(unsubscribeJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Incorrect unsubscribe query: wrong channel"){
    val unsubscribePath = "../protocol/examples/query/unsubscribe/wrong_unsubscribe_channel.json"
    val unsubscribeJson = getJsonStringFromFile(unsubscribePath)

    validateSchema(unsubscribeJson) shouldBe a [Right[_,PipelineError]]
  }
}
