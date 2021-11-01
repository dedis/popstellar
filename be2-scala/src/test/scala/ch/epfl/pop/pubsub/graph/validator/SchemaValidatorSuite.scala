package ch.epfl.pop.pubsub.graph.validator

import org.scalatest.{Matchers, FunSuite}
import scala.io.Source
import ch.epfl.pop.pubsub.graph.Validator._
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.pubsub.graph.ErrorCodes

class SchemaValidatorSuite extends FunSuite with Matchers {

  /* Test subscribe query without message format*/
  test("Correct subscribe JSON-RPC query"){
    val subscribePath = "../protocol/examples/query/subscribe/subscribe.json"
    val source = Source.fromFile(subscribePath);
    val subscribeJson = source.getLines.mkString
    source.close

    validateSchema(subscribeJson) should be (Left(subscribeJson))
  }

  test("Incorrect subscribe query: additional params"){
    val subscribePath = "../protocol/examples/query/subscribe/wrong_subscribe__additional_params.json"
    val source = Source.fromFile(subscribePath);
    val subscribeJson = source.getLines.mkString
    source.close

    validateSchema(subscribeJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Incorrect subscribe query: missing channel"){
    val subscribePath = "../protocol/examples/query/subscribe/wrong_subscribe_missing_channel.json"
    val source = Source.fromFile(subscribePath);
    val subscribeJson = source.getLines.mkString
    source.close

    validateSchema(subscribeJson) shouldBe a [Right[_,PipelineError]]
  }

  /* Test broadcast query with message format */
  test("Correct broadcast JSON-RPC query"){
    val broadcastPath = "../protocol/examples/query/broadcast/broadcast.json"
    val source = Source.fromFile(broadcastPath);
    val broadcastJson = source.getLines.mkString
    source.close

    validateSchema(broadcastJson) should be (Left(broadcastJson))
  }

  test("Incorrect broadcast query: additional params"){
    val broadcastPath = "../protocol/examples/query/broadcast/wrong_broadcast_additional_params.json"
    val source = Source.fromFile(broadcastPath);
    val broadcastJson = source.getLines.mkString
    source.close

    validateSchema(broadcastJson) shouldBe a [Right[_,PipelineError]]
  }

  test("Incorrect broadcast query: missing message"){
    val broadcastPath = "../protocol/examples/query/broadcast/wrong_broadcast_missing_message.json"
    val source = Source.fromFile(broadcastPath);
    val broadcastJson = source.getLines.mkString
    source.close

    validateSchema(broadcastJson) shouldBe a [Right[_,PipelineError]]
  }


}
