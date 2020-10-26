package json

import org.scalatest.FunSuite

class JsonMessageParserTest extends FunSuite {

  val parser = new JsonMessageParser

  val sourceCreate: String = """{ "create":{ "channel":"General Channel", "contract":"contract_type" } }"""
  val sourceSubscribe: String = """{"subscribe":{"channel":"Breakout Room Channel 1"}}"""


  test("json.JsonMessageParser.parseMessage") {
    val messageCreate: JsonMessages.JsonMessage = parser.parseMessage(sourceCreate)
    val messageSubscribe: JsonMessages.JsonMessage = parser.parseMessage(sourceSubscribe)

    assert(messageCreate === JsonMessages.CreateChannelClient("General Channel", "contract_type"))
    assert(messageSubscribe === JsonMessages.SubscribeChannelClient("Breakout Room Channel 1"))
  }

  test("json.JsonMessageParser.serializeMessage") {
    val sourceFetchServer: String = """{
      "event":{
        "channel":"channel1",
        "event_content":"RollCall",
        "event_id":"event#13"
      }
    }""".filterNot((c: Char) => c.isWhitespace)

    val messageFetch: JsonMessages.JsonMessage = JsonMessages.FetchChannelServer("channel1", "event#13", "RollCall")
    val messageFetchFaulty: JsonMessages.JsonMessage = JsonMessages.FetchChannelServer("channel1", "event#14", "RollCall")

    assert(parser.serializeMessage(messageFetch) === sourceFetchServer)
    assert(!(sourceFetchServer === parser.serializeMessage(messageFetchFaulty)))
  }

}
