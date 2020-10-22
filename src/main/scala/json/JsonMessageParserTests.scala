package json

import JsonMessages._

object JsonMessageParserTests extends App {

  // Parser Simulation
  val sourceCreate: String = """{ "create":{ "channel":"General Channel", "contract":"contract_type" } }"""
  val sourceSubscribe: String = """{"subscribe":{"channel":"Breakout Room Channel 1"}}"""

  val parser = new JsonMessageParser

  val source: String = sourceSubscribe
  val message: JsonMessage = parser.parseMessage(source)


  // Input checker simulation : checks that input from the client are not wrong
  def check(m: JsonMessage): Boolean = true
  assert(check(message))


  // Partitioner simulation : forwards messages where they need to be sent
  message match {
    case _: JsonMessageAdminClient => println("partitioner detects a JsonMessageClient")

    case _: JsonMessageAdminServer => println("partitioner detects a JsonMessageServer")

    case _: JsonMessagePubSub => message match {
      case _: JsonMessagePubSubClient => message match {
        case CreateChannelClient(cn, _) => println(s"forwarding creating channel '${cn}' request to next component...")
        case SubscribeChannelClient(cn) => println(s"forwarding subbing to '${cn}' message to next component...")
        case _ => println("Unrecognized JsonMessagePubSubClient message")
      }
      case _: JsonMessagePubSubServer => println("partitioner detects a JsonMessagePubSubServer")
    }

    case _ => println(s"partitioner is confused! The following message was not recognized :\n${message.toString}")
  }


  // Server sends ACK to client simulation
  val answer: JsonMessage = AnswerMessageServer(success = true, None)
  println(parser.encodeMessage(answer))

}

