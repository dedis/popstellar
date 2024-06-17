package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers
import util.examples.Federation.FederationTokensExchangeExample.*
import spray.json.*

class TokensExchangeSuite extends FunSuite with Matchers {

  test("Constructor/apply works for TokensExchange as expected") {
    TOKENS_EXCHANGE.laoId should equal(LAO_ID)
    TOKENS_EXCHANGE.rollCallId should equal(ROLL_CALL_ID)
    TOKENS_EXCHANGE.tokens should equal(TOKENS)
    TOKENS_EXCHANGE.timestamp should equal(TIMESTAMP)
    TOKENS_EXCHANGE._object should equal(ObjectType.federation)
    TOKENS_EXCHANGE.action should equal(ActionType.tokens_exchange)
  }

  test("BuildFromJson for TokensExchange works as expected") {
    val tokensExchange_ = FederationTokensExchange.buildFromJson(TOKENS_EXCHANGE.toJson.toString)
  }

}
