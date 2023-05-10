package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample.{AUTHENTICATE_INVALID_CHANNEL_RPC, AUTHENTICATE_INVALID_RESPONSE_MODE_RPC, AUTHENTICATE_INVALID_SIGNATURE_RPC, AUTHENTICATE_OTHER_RESPONSE_MODE_RPC, AUTHENTICATE_RPC}

class PopchaValidatorSuite extends FunSuite with Matchers with AskPatternConstants {
  test("Authenticate works") {
    val message: GraphMessage = PopchaValidator.validateAuthenticateRequest(AUTHENTICATE_RPC)
    message should equal(Right(AUTHENTICATE_RPC))
  }

  test("Authenticate works with other response mode") {
    val message: GraphMessage = PopchaValidator.validateAuthenticateRequest(AUTHENTICATE_OTHER_RESPONSE_MODE_RPC)
    message should equal(Right(AUTHENTICATE_OTHER_RESPONSE_MODE_RPC))
  }

  test("Authenticate with wrong channel fails") {
    val message: GraphMessage = PopchaValidator.validateAuthenticateRequest(AUTHENTICATE_INVALID_CHANNEL_RPC)
    message shouldBe a[Left[_, PipelineError]]
  }

  test("Authenticate with wrong signature fails") {
    val message: GraphMessage = PopchaValidator.validateAuthenticateRequest(AUTHENTICATE_INVALID_SIGNATURE_RPC)
    message shouldBe a[Left[_, PipelineError]]
  }

  test("Authenticate with wrong response mode fails") {
    val message: GraphMessage = PopchaValidator.validateAuthenticateRequest(AUTHENTICATE_INVALID_RESPONSE_MODE_RPC)
    message shouldBe a[Left[_, PipelineError]]
  }
}
