package json

import java.util.Base64

import ch.epfl.pop.json.JsonCommunicationProtocol.AnswerArrayMessageServerFormat
import ch.epfl.pop.json.JsonMessages._
import ch.epfl.pop.json._
import org.scalatest.FunSuite

class JsonMessageParserTest extends FunSuite {

  val ERROR_MESSAGE: String = "error_for_error_code_above"

  val MessageContentExample: String = """{
                                        |            "data": "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJ1cGRhdGVfcHJvcGVydGllcyIsImlkIjoiMHhhYWEiLCJuYW1lIjoiTWEgTGFvIiwiY3JlYXRpb24iOjQ1NDUsImxhc3RfbW9kaWZpZWQiOjQ1NDUsIm9yZ2FuaXplciI6IjB4YmIiLCJ3aXRuZXNzZXMiOlsiMHgxMiIsICIweDEzIl19",
                                        |            "sender": "0x530dE8",
                                        |            "signature": "0x5100",
                                        |            "message_id": "0x1d0",
                                        |            "witness_signatures": ["0xceb1", "0xceb2", "0xceb3"]
                                        |        }""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val _dataLao: String = s"""{
                                  |    "object": "${Objects.Lao.toString}",
                                  |    "action": "F_ACTION",
                                  |    "id": "0x999",
                                  |    "name": "name",
                                  |    "creation": 222,
                                  |    "last_modified": 222,
                                  |    "organizer": "0x909",
                                  |    "witnesses": ["0x111"]
                                  |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)

  val _dataMeeting: String = s"""{
                                  |    "object": "${Objects.Meeting.toString}",
                                  |    "action": "F_ACTION",
                                  |    "id": "0x888",
                                  |    "name": "nameMeeting",
                                  |    "creation": 333,
                                  |    "last_modified": 333,
                                  |    "location": "Paris",
                                  |    "start": 400,
                                  |    "end": 500,
                                  |    "extra": "extra_stuff"
                                  |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)


  val dataUpdateLao: String = s"""{
                                 |    "object": "${Objects.Lao.toString}",
                                 |    "action": "${Actions.UpdateProperties.toString}",
                                 |    "name": "name6",
                                 |    "last_modified": 2226,
                                 |    "witnesses": ["0x111", "0x1116"]
                                 |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)

  val dataWitnessMessage: String = s"""{
                                      |    "object": "${Objects.Message.toString}",
                                      |    "action": "${Actions.Witness.toString}",
                                      |    "message_id": "0xbeef",
                                      |    "signature": "0x9090"
                                      |}""".stripMargin.filterNot((c: Char) => c.isWhitespace)

  val dataCreateLao: String = _dataLao.replaceAll("F_ACTION", Actions.Create.toString)
  val dataBroadcastLao: String = _dataLao.replaceAll("F_ACTION", Actions.State.toString)
  val dataCreateMeeting: String = _dataMeeting.replaceAll("F_ACTION", Actions.Create.toString)
  val dataBroadcastMeeting: String = _dataMeeting.replaceAll("F_ACTION", Actions.State.toString)


  def embeddedMessage(
                        data: String,
                        method: Methods.Methods = Methods.Publish,
                        channel: ChannelName = "/root/lao_id",
                        id: Int = 0
                      ): String = {
                        s"""{
                           |    "jsonrpc": "2.0",
                           |    "method": "${method.toString}",
                           |    "params": {
                           |        "channel": "$channel",
                           |        "message": {
                           |            "data": "${Base64.getEncoder.encode(data.getBytes).map(_.toChar).mkString}",
                           |            "sender": "0x530dE8",
                           |            "signature": "0x5100",
                           |            "message_id": "0x1d0",
                           |            "witness_signatures": ["0xceb1", "0xceb2", "0xceb3"]
                           |        }
                           |    },
                           |    "id": $id
                           |  }
                           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)
  }

  def embeddedServerAnswer(result: Option[Int], error: Option[String] = Some(ERROR_MESSAGE), code: Int = 0, id: Int = 0): String = {
    (result, error) match {
      case (Some(r), None) =>
        val additional: String = s""""result": $r"""
        s"""{
           |    "id": $id,
           |    "jsonrpc": "2.0",
           |    $additional
           |  }
           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)
      case (None, Some(e)) =>
        val additional: String = s""""error": {"code": $code, "description": "$e"}"""
        s"""{
           |    $additional,
           |    "id": $id,
           |    "jsonrpc": "2.0"
           |  }
           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)
      case _ => throw new IllegalArgumentException("Impossible argument combination in embeddedServerAnswer")
    }
  }


  test("JsonMessageParser.parseMessage|encodeMessage:CreateLaoMessageClient") {
    val source: String = embeddedMessage(dataCreateLao, channel = "/root")
    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[CreateLaoMessageClient])
    assert(spdp.isInstanceOf[CreateLaoMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:UpdateLaoMessageClient") {
    val source: String = embeddedMessage(dataUpdateLao)
    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[UpdateLaoMessageClient])
    assert(spdp.isInstanceOf[UpdateLaoMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:BroadcastLaoMessageClient") {
    val source: String = embeddedMessage(dataBroadcastLao)
    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[BroadcastLaoMessageClient])
    assert(spdp.isInstanceOf[BroadcastLaoMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:WitnessMessageMessageClient") {
    val source: String = embeddedMessage(dataWitnessMessage)
    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[WitnessMessageMessageClient])
    assert(spdp.isInstanceOf[WitnessMessageMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:CreateMeetingMessageClient") {
    val source: String = embeddedMessage(dataCreateMeeting)
    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[CreateMeetingMessageClient])
    assert(spdp.isInstanceOf[CreateMeetingMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:BroadcastMeetingMessageClient") {
    val source: String = embeddedMessage(dataBroadcastMeeting)
    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[BroadcastMeetingMessageClient])
    assert(spdp.isInstanceOf[BroadcastMeetingMessageClient])
  }



  test("JsonMessageParser.parseMessage|encodeMessage:SubscribeMessageClient") {
    val source: String = """{
                           |    "jsonrpc": "2.0",
                           |    "method": "subscribe",
                           |    "params": {
                           |        "channel": "channel_id"
                           |    },
                           |    "id": 3
                           |  }
                           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[SubscribeMessageClient])
    assert(spdp.isInstanceOf[SubscribeMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:UnsubscribeMessageClient") {
    val source: String = """{
                           |    "jsonrpc": "2.0",
                           |    "method": "unsubscribe",
                           |    "params": {
                           |        "channel": "channel_id"
                           |    },
                           |    "id": 3
                           |  }
                           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[UnsubscribeMessageClient])
    assert(spdp.isInstanceOf[UnsubscribeMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:PropagateMessageClient") {
    val source: String = s"""{
                           |    "jsonrpc": "2.0",
                           |    "method": "message",
                           |    "params": {
                           |        "channel": "channel_id",
                           |        "message": $MessageContentExample
                           |    }
                           |  }
                           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[PropagateMessageClient])
    assert(spdp.isInstanceOf[PropagateMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:CatchupMessageClient") {
    val source: String = """{
                           |    "jsonrpc": "2.0",
                           |    "method": "catchup",
                           |    "params": {
                           |        "channel": "channel_id"
                           |    },
                           |    "id": 3
                           |  }
                           |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    val sp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(source)

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessages.JsonMessage = JsonMessageParser.parseMessage(spd)

    assert(sp === spdp)
    assert(sp.isInstanceOf[CatchupMessageClient])
    assert(spdp.isInstanceOf[CatchupMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:AnswerIntMessageServer") {

    // Success
    val source: String = embeddedServerAnswer(Some(0), None, id = 13)

    val sp: JsonMessages.JsonMessage = AnswerIntMessageServer("2.0", Some(0), None, 13)
    val spd: String = JsonMessageParser.serializeMessage(sp).filterNot((c: Char) => c.isWhitespace)

    assertResult(source)(spd)


    // Failure
    for (i <- -5 until 0) {
      val source: String = embeddedServerAnswer(None, code = i, id = 3 * i)

      val sp: JsonMessages.JsonMessage = AnswerIntMessageServer("2.0", None, Some(MessageErrorContent(i, ERROR_MESSAGE)), 3 * i)
      val spd: String = JsonMessageParser.serializeMessage(sp)

      assertResult(source)(spd)
    }
  }

  test("JsonMessageParser.parseMessage|encodeMessage:AnswerArrayMessageServer") {
    // Success
    val source: String = s"""{
                            |    "id": 99,
                            |    "jsonrpc": "2.0",
                            |    "result": {
                            |       "messages": ["M1", "M2", "M3"]
                            |    }
                            |  }
                            |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    val sp: JsonMessages.JsonMessage = AnswerArrayMessageServer("2.0", Some(List("M1", "M2", "M3")), None, 99)
    val spd: String = JsonMessageParser.serializeMessage(sp)

    assertResult(source)(spd)


    // Failure
    for (i <- -5 until 0) {
      val source: String = embeddedServerAnswer(None, code = i, id = 3 * i)

      val sp: JsonMessages.JsonMessage = AnswerArrayMessageServer("2.0", None, Some(MessageErrorContent(i, ERROR_MESSAGE)), 3 * i)
      val spd: String = JsonMessageParser.serializeMessage(sp)

      assertResult(source)(spd)
    }
  }
}
