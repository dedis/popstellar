package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample.AUTHENTICATE_RPC

class PopchaValidatorSuite extends FunSuite with Matchers with AskPatternConstants {
  test("Authenticate works") {
    val message: GraphMessage = PopchaValidator.validateAuthenticateRequest(AUTHENTICATE_RPC)
    message should equal(Right(AUTHENTICATE_RPC))
  }
}
