package json

import java.util.Base64

import ch.epfl.pop.json.JsonMessages.{JsonMessagePublishClient, _}
import ch.epfl.pop.json.JsonUtils.{JsonMessageParserException, MessageContentDataBuilder}
import ch.epfl.pop.json._
import spray.json._
import ch.epfl.pop.json.JsonCommunicationProtocol._
import org.scalatest.{FunSuite, Matchers}
import JsonParserTestsUtils._


class JsonMessageParserTest extends FunSuite with Matchers {


  implicit class RichJsonMessage(m: JsonMessage) {
    def shouldBeEqualUntilMessageContent(o: JsonMessage): Unit = {

      @scala.annotation.tailrec
      def checkListOfByteArray(l1: List[Array[Byte]], l2: List[Array[Byte]]): Unit = {
        l1.length should equal (l2.length)

        (l1, l2) match {
          case _ if l1.isEmpty =>
          case (h1 :: tail1, h2 :: tail2) =>
            h1 should equal (h2)
            checkListOfByteArray(tail1, tail2)
        }
      }


      val o1 = this.m
      val o2 = o

      o1 match {
        case _: PropagateMessageServer =>
          o2 shouldBe a [PropagateMessageServer]

          val o11 = o1.asInstanceOf[PropagateMessageServer]
          val o22 = o2.asInstanceOf[PropagateMessageServer]

          val a1 = CreateLaoMessageClient(o11.params, -1, o11.method, o11.jsonrpc)
          val a2 = CreateLaoMessageClient(o22.params, -1, o22.method, o22.jsonrpc)

          a1 shouldBeEqualUntilMessageContent a2


        case _: JsonMessagePublishClient =>
          o2 shouldBe a [JsonMessagePublishClient]

          val o11 = o1.asInstanceOf[JsonMessagePublishClient]
          val o22 = o2.asInstanceOf[JsonMessagePublishClient]

          o11.jsonrpc should equal(o22.jsonrpc)
          o11.id should equal(o22.id)
          o11.method should equal(o22.method)

          o11.params.channel should equal (o22.params.channel)
          (o11.params.message, o22.params.message) match {
            case (None, None) =>

            case (Some(mc1), Some(mc2)) =>
              mc1.sender should equal (mc2.sender)
              mc1.signature should equal (mc2.signature)
              mc1.message_id should equal (mc2.message_id)
              checkListOfByteArray(mc1.witness_signatures, mc2.witness_signatures)

              mc1.data._object should equal (mc2.data._object)
              mc1.data.action should equal (mc2.data.action)
              mc1.data.id should equal (mc2.data.id)
              mc1.data.name should equal (mc2.data.name)
              mc1.data.creation should equal (mc2.data.creation)
              mc1.data.last_modified should equal (mc2.data.last_modified)
              mc1.data.organizer should equal (mc2.data.organizer)
              checkListOfByteArray(mc1.data.witnesses, mc2.data.witnesses)
              mc1.data.message_id should equal (mc2.data.message_id)
              mc1.data.signature should equal (mc2.data.signature)
              mc1.data.location should equal (mc2.data.location)
              mc1.data.start should equal (mc2.data.start)
              mc1.data.end should equal (mc2.data.end)
              mc1.data.extra should equal (mc2.data.extra)

            case _ => fail()
          }

        case _ => throw new UnsupportedOperationException
      }
    }
  }


  @scala.annotation.tailrec
  final def listStringify(value: List[Key], acc: String = ""): String = {
    if (value.nonEmpty) {
      var sep = ""
      if (value.length > 1) sep = ","

      listStringify(value.tail, acc + "\"" + encodeBase64String(value.head.map(_.toChar).mkString) + "\"" + sep)
    }
    else "[" + acc + "]"
  }


  test("JsonMessageParser.parseMessage|encodeMessage:CreateLaoMessageClient") {
    val source: String = embeddedMessage(dataCreateLao, channel = "/root")
    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [CreateLaoMessageClient]
    spdp shouldBe a [CreateLaoMessageClient]
    sp shouldBeEqualUntilMessageContent spdp
  }

  test("JsonMessageParser.parseMessage|encodeMessage:UpdateLaoMessageClient") {
    val source: String = embeddedMessage(dataUpdateLao)
    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [UpdateLaoMessageClient]
    spdp shouldBe a [UpdateLaoMessageClient]
    sp shouldBeEqualUntilMessageContent spdp
  }

  test("JsonMessageParser.parseMessage|encodeMessage:BroadcastLaoMessageClient") {
    val source: String = embeddedMessage(dataBroadcastLao)
    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [BroadcastLaoMessageClient]
    spdp shouldBe a [BroadcastLaoMessageClient]
    sp shouldBeEqualUntilMessageContent spdp
  }

  test("JsonMessageParser.parseMessage|encodeMessage:WitnessMessageMessageClient") {
    val source: String = embeddedMessage(dataWitnessMessage)
    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [WitnessMessageMessageClient]
    spdp shouldBe a [WitnessMessageMessageClient]
    sp shouldBeEqualUntilMessageContent spdp
  }

  test("JsonMessageParser.parseMessage|encodeMessage:CreateMeetingMessageClient") {
    // Meeting with every argument
    var data: String = dataCreateMeeting
    var sp: JsonMessage = JsonMessageParser.parseMessage(embeddedMessage(data)) match {
      case Left(m) => m
      case _ => fail()
    }

    var spd: String = JsonMessageParser.serializeMessage(sp)
    var spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [CreateMeetingMessageClient]
    spdp shouldBe a [CreateMeetingMessageClient]
    sp shouldBeEqualUntilMessageContent spdp


    // Meeting without location
    data = data.replaceAll(",\"location\":\"[a-zA-Z]*\"", "")
    sp = JsonMessageParser.parseMessage(embeddedMessage(data)) match {
      case Left(m) => m
      case _ => fail()
    }

    spd = JsonMessageParser.serializeMessage(sp)
    spdp = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [CreateMeetingMessageClient]
    spdp shouldBe a [CreateMeetingMessageClient]
    sp shouldBeEqualUntilMessageContent spdp

    // Meeting without location and end
    data = data.replaceAll(",\"end\":[0-9]*", "")
    sp = JsonMessageParser.parseMessage(embeddedMessage(data)) match {
      case Left(m) => m
      case _ => fail()
    }

    spd = JsonMessageParser.serializeMessage(sp)
    spdp = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [CreateMeetingMessageClient]
    spdp shouldBe a [CreateMeetingMessageClient]
    sp shouldBeEqualUntilMessageContent spdp

    // Meeting without location, end and extra
    data = data.replaceAll(",\"extra\":\"[a-zA-Z0-9_]*\"", "")
    sp = JsonMessageParser.parseMessage(embeddedMessage(data)) match {
      case Left(m) => m
      case _ => fail()
    }

    spd = JsonMessageParser.serializeMessage(sp)
    spdp = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [CreateMeetingMessageClient]
    spdp shouldBe a [CreateMeetingMessageClient]
    sp shouldBeEqualUntilMessageContent spdp

    // Meeting without start (should not work)
    data = data.replaceAll(",\"start\":[0-9]*", "")
    try {
      sp = JsonMessageParser.parseMessage(embeddedMessage(data)) match {
        case Left(_) => fail()
        case Right(_) => throw JsonMessageParserException("")
      }
    }
    catch { case _: JsonMessageParserException => }
  }

  test("JsonMessageParser.parseMessage|encodeMessage:BroadcastMeetingMessageClient") {
    val source: String = embeddedMessage(dataBroadcastMeeting)
    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [BroadcastMeetingMessageClient]
    spdp shouldBe a [BroadcastMeetingMessageClient]
    sp shouldBeEqualUntilMessageContent spdp
  }

  test("JsonMessageParser.parseMessage|encodeMessage:PropagateMessageServer") {
    val source: String = s"""{
                            |    "jsonrpc": "2.0",
                            |    "method": "message",
                            |    "params": {
                            |        "channel": "channel_id",
                            |        "message": $MessageContentExample
                            |    }
                            |  }
                            |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    sp shouldBe a [PropagateMessageServer]
    spdp shouldBe a [PropagateMessageServer]
    sp shouldBeEqualUntilMessageContent spdp
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

    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

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

    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    assert(sp === spdp)
    assert(sp.isInstanceOf[UnsubscribeMessageClient])
    assert(spdp.isInstanceOf[UnsubscribeMessageClient])
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

    val sp: JsonMessage = JsonMessageParser.parseMessage(source) match {
      case Left(m) => m
      case _ => fail()
    }

    val spd: String = JsonMessageParser.serializeMessage(sp)
    val spdp: JsonMessage = JsonMessageParser.parseMessage(spd) match {
      case Left(m) => m
      case _ => fail()
    }

    assert(sp === spdp)
    assert(sp.isInstanceOf[CatchupMessageClient])
    assert(spdp.isInstanceOf[CatchupMessageClient])
  }

  test("JsonMessageParser.parseMessage|encodeMessage:AnswerResultIntMessageServer") {
    val source: String = embeddedServerAnswer(Some(0), None, id = 13)

    val sp: JsonMessages.JsonMessage = AnswerResultIntMessageServer(13, 0,  "2.0")
    val spd: String = JsonMessageParser.serializeMessage(sp).filterNot((c: Char) => c.isWhitespace)

    assertResult(source)(spd)
  }

  test("JsonMessageParser.parseMessage|encodeMessage:AnswerResultArrayMessageServer") {
    val source: String = s"""{
                            |    "id": 99,
                            |    "jsonrpc": "2.0",
                            |    "result": {
                            |       "messages": F_MESSAGES
                            |    }
                            |  }
                            |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    var sp: JsonMessages.JsonMessage = AnswerResultArrayMessageServer(99, ChannelMessages(List()))
    var spd: String = JsonMessageParser.serializeMessage(sp)

    assertResult(source.replaceAll("F_MESSAGES", "[]"))(spd)


    // 1 message and empty witness list
    val data: MessageContentData = new MessageContentDataBuilder().setHeader(Objects.Message, Actions.Witness).setId("2".getBytes).setStart(22).build()
    var m: MessageContent = MessageContent(data, "skey".getBytes, "sign".getBytes, "mid".getBytes, List())
    sp = AnswerResultArrayMessageServer(99, ChannelMessages(List(m)))
    spd = JsonMessageParser.serializeMessage(sp)

    val rd: String = """eyJvYmplY3QiOiJtZXNzYWdlIiwiYWN0aW9uIjoid2l0bmVzcyIsImlkIjoiTWc9PSIsInN0YXJ0IjoyMn0="""
    var r: String = s"""[{"data":"$rd","message_id":"bWlk","sender":"c2tleQ==","signature":"c2lnbg==","witness_signatures":[]}]"""

    assertResult(source.replaceAll("F_MESSAGES", r))(spd)
    assert(rd === Base64.getEncoder.encode(data.toJson.toString().getBytes).map(_.toChar).mkString)
    assert(Base64.getDecoder.decode(rd).map(_.toChar).mkString === data.toJson.toString())


    // 1 message and non-empty witness list
    val sig: List[Key] = List("witnessKey1".getBytes, "witnessKey2".getBytes, "witnessKey3".getBytes)
    m = MessageContent(data, "skey".getBytes, "sign".getBytes, "mid".getBytes, sig)
    sp = AnswerResultArrayMessageServer(99, ChannelMessages(List(m)))
    spd = JsonMessageParser.serializeMessage(sp)

    r = s"""[{"data":"$rd","message_id":"bWlk","sender":"c2tleQ==","signature":"c2lnbg==","witness_signatures":${listStringify(sig)}}]"""

    assertResult(source.replaceAll("F_MESSAGES", r))(spd)
    assert(rd === Base64.getEncoder.encode(data.toJson.toString().getBytes).map(_.toChar).mkString)
    assert(Base64.getDecoder.decode(rd).map(_.toChar).mkString === data.toJson.toString())
  }

  test("JsonMessageParser.parseMessage|encodeMessage:AnswerErrorMessageServer") {
    val source: String = s"""{
                            |    "error": {
                            |       "code": ERR_CODE,
                            |       "description": "err"
                            |    },
                            |    "id": 99,
                            |    "jsonrpc": "2.0"
                            |  }
                            |""".stripMargin.filterNot((c: Char) => c.isWhitespace)

    for (i <- -5 until 0) {
      val sp: JsonMessages.JsonMessage = AnswerErrorMessageServer(99, MessageErrorContent(i, "err"),  "2.0")
      val spd: String = JsonMessageParser.serializeMessage(sp)

      assertResult(source.replaceAll("ERR_CODE", String.valueOf(i)))(spd)
    }
  }
}



